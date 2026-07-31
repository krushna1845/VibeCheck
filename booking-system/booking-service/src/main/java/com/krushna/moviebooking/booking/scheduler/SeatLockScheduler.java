package com.krushna.moviebooking.booking.scheduler;

import com.krushna.moviebooking.booking.config.SeatLockProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Background scheduler monitoring Redis seat locks telemetry and auto-expiration health metrics.
 *
 * <p>Operates strictly in-memory on Redis key spaces — no database writes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatLockScheduler {

    private final StringRedisTemplate stringRedisTemplate;
    private final SeatLockProperties seatLockProperties;

    /**
     * Periodically inspects active seat lock keys in Redis and logs statistics.
     */
    @Scheduled(fixedRateString = "${booking.seat-lock.scheduler-interval-ms:60000}")
    public void monitorActiveSeatLocks() {
        String pattern = (seatLockProperties != null && seatLockProperties.getKeyPrefix() != null)
                ? seatLockProperties.getKeyPrefix() + "*"
                : "seat:*";

        try {
            Set<String> keys = stringRedisTemplate.keys(pattern);
            int activeLockCount = keys != null ? keys.size() : 0;
            log.info("SeatLockScheduler Audit: Currently {} active seat locks stored in Redis key pattern '{}'",
                    activeLockCount, pattern);
        } catch (Exception e) {
            log.warn("SeatLockScheduler Error: Failed to inspect Redis keys matching pattern '{}'", pattern, e);
        }
    }
}
