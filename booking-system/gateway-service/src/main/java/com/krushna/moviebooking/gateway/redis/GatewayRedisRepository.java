package com.krushna.moviebooking.gateway.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed rate limiter for gateway.
 * Tracks requests per IP/user with a sliding window counter.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class GatewayRedisRepository {

    private static final String RATE_LIMIT_PREFIX = "gateway:rate:";

    private final StringRedisTemplate redisTemplate;

    @Value("${rate-limit.max-requests-per-minute:60}")
    private int maxRequestsPerMinute;

    /**
     * Increment the request count for a given key and return true if within limits.
     *
     * @param clientKey e.g. userId or IP address
     */
    public boolean isWithinRateLimit(String clientKey) {
        String key = RATE_LIMIT_PREFIX + clientKey;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }

        long current = Optional.ofNullable(count).orElse(0L);

        if (current > maxRequestsPerMinute) {
            log.warn("[Gateway] Rate limit exceeded for key='{}' count={}", clientKey, current);
            return false;
        }
        return true;
    }

    public long getCurrentRequestCount(String clientKey) {
        String value = redisTemplate.opsForValue().get(RATE_LIMIT_PREFIX + clientKey);
        return value != null ? Long.parseLong(value) : 0L;
    }
}
