package com.krushna.moviebooking.booking.scheduler;

import com.krushna.moviebooking.booking.config.SeatLockProperties;
import com.krushna.moviebooking.booking.entity.Booking;
import com.krushna.moviebooking.booking.repository.BookingRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Background scheduler monitoring Redis seat lock telemetry, inspecting active keys,
 * and performing periodic cleanup of orphan or expired seat locks.
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

    /**
     * Periodically inspects active seat lock keys in Redis, cleans up orphan locks, and records metrics.
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
            Set<String> keys = stringRedisTemplate.keys(pattern);
            int activeLockCount = keys != null ? keys.size() : 0;
            log.info("SeatLockCleanupScheduler Audit: Found {} active seat lock keys in Redis pattern '{}'",
                    activeLockCount, pattern);

            meterRegistry.gauge("seatlock.active.count", activeLockCount);

            int orphanCleanedCount = 0;
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    Long ttl = stringRedisTemplate.getExpire(key);
                    // TTL -1 indicates key exists without an expiration TTL set (orphan)
                    if (ttl != null && ttl == -1) {
                        log.warn("Found orphan Redis seat lock without TTL: {}. Removing key.", key);
                        stringRedisTemplate.delete(key);
                        orphanCleanedCount++;
                    }
                }
            }

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
