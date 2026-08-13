package com.krushna.moviebooking.booking.scheduler;

import com.krushna.moviebooking.booking.config.SeatLockProperties;
import com.krushna.moviebooking.booking.entity.Booking;
import com.krushna.moviebooking.booking.repository.BookingRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Background scheduler monitoring Redis seat lock telemetry, inspecting active keys,
 * and performing periodic cleanup of orphan or expired seat locks via SCAN cursor.
 *
 * <p>Runs every 60 seconds by default.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatLockCleanupScheduler {

    private final StringRedisTemplate stringRedisTemplate;
    private final SeatLockProperties seatLockProperties;
    private final BookingRepository bookingRepository;
    private final MeterRegistry meterRegistry;

    private record CleanupResult(int totalKeys, int orphansCleaned) {}

    /**
     * Periodically inspects active seat lock keys in Redis via SCAN, cleans up orphan locks, and records metrics.
     */
    @Scheduled(fixedRateString = "${booking.scheduler.seat-lock-cleanup-rate-ms:60000}")
    public void cleanupOrphanSeatLocks() {
        Timer.Sample sample = Timer.start(meterRegistry);

        Counter.builder("seatlock.cleanup.runs")
                .description("Number of seat lock cleanup runs executed")
                .register(meterRegistry)
                .increment();

        String pattern = (seatLockProperties != null && seatLockProperties.getKeyPrefix() != null)
                ? seatLockProperties.getKeyPrefix() + "*"
                : "seat:*";

        try {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(1000).build();

            CleanupResult result = stringRedisTemplate.execute((RedisCallback<CleanupResult>) connection -> {
                int totalKeys = 0;
                int orphans = 0;
                try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                    while (cursor.hasNext()) {
                        totalKeys++;
                        String key = new String(cursor.next(), StandardCharsets.UTF_8);
                        Long ttl = stringRedisTemplate.getExpire(key);
                        // TTL -1 indicates key exists without an expiration TTL set (orphan)
                        if (ttl != null && ttl == -1) {
                            log.warn("Found orphan Redis seat lock without TTL: {}. Removing key.", key);
                            stringRedisTemplate.delete(key);
                            orphans++;
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Error processing SCAN cursor in SeatLockCleanupScheduler: {}", ex.getMessage());
                }
                return new CleanupResult(totalKeys, orphans);
            });

            int activeLockCount = result != null ? result.totalKeys() : 0;
            int orphanCleanedCount = result != null ? result.orphansCleaned() : 0;

            log.info("SeatLockCleanupScheduler Audit: Found {} active seat lock keys in Redis pattern '{}'",
                    activeLockCount, pattern);

            meterRegistry.gauge("seatlock.active.count", activeLockCount);

            if (orphanCleanedCount > 0) {
                Counter.builder("seatlock.orphan.cleaned")
                        .description("Count of orphan seat locks cleaned up from Redis")
                        .register(meterRegistry)
                        .increment(orphanCleanedCount);
                log.info("SeatLockCleanupScheduler: Successfully cleaned up {} orphan seat locks from Redis.", orphanCleanedCount);
            }
        } catch (Exception e) {
            log.warn("SeatLockCleanupScheduler Error: Failed to inspect or clean Redis keys matching pattern '{}'", pattern, e);
        } finally {
            sample.stop(Timer.builder("seatlock.cleanup.duration")
                    .description("Time taken to run seat lock cleanup task")
                    .register(meterRegistry));
        }
    }
}
