package com.krushna.moviebooking.booking.scheduler;

import com.krushna.moviebooking.booking.config.SeatLockProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Background scheduler monitoring Redis seat locks telemetry and auto-expiration health metrics.
 *
 * <p>Operates strictly in-memory on Redis key spaces using non-blocking SCAN iteration — no database writes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatLockScheduler {

    private final StringRedisTemplate stringRedisTemplate;
    private final SeatLockProperties seatLockProperties;

    /**
     * Periodically inspects active seat lock keys in Redis via SCAN and logs statistics.
     */
    @Scheduled(fixedRateString = "${booking.seat-lock.scheduler-interval-ms:60000}")
    public void monitorActiveSeatLocks() {
        String pattern = (seatLockProperties != null && seatLockProperties.getKeyPrefix() != null)
                ? seatLockProperties.getKeyPrefix() + "*"
                : "seat:*";

        try {
            int activeLockCount = countKeysWithScan(pattern);
            log.info("SeatLockScheduler Audit: Currently {} active seat locks stored in Redis key pattern '{}'",
                    activeLockCount, pattern);
        } catch (Exception e) {
            log.warn("SeatLockScheduler Error: Failed to inspect Redis keys matching pattern '{}'", pattern, e);
        }
    }

    private int countKeysWithScan(String pattern) {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(1000).build();
        Integer count = stringRedisTemplate.execute((RedisCallback<Integer>) connection -> {
            int total = 0;
            try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    cursor.next();
                    total++;
                }
            } catch (Exception ex) {
                log.warn("Error closing Redis SCAN cursor: {}", ex.getMessage());
            }
            return total;
        });
        return count != null ? count : 0;
    }
}
