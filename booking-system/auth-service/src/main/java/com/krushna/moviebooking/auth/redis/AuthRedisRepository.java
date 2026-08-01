package com.krushna.moviebooking.auth.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed repository for short-lived auth data.
 *
 * Use cases:
 * - Token blacklisting (after explicit logout before expiry)
 * - Rate-limit counters for login attempts
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AuthRedisRepository {

    private static final String BLACKLIST_PREFIX = "auth:blacklist:";
    private static final String RATE_LIMIT_PREFIX = "auth:rate_limit:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Blacklist a JWT access token so it cannot be used even if not expired.
     */
    public void blacklistToken(String tokenHash, Duration ttl) {
        String key = BLACKLIST_PREFIX + tokenHash;
        redisTemplate.opsForValue().set(key, "1", ttl);
        log.debug("[AuthRedis] Token blacklisted key={}", key);
    }

    /**
     * Check if a token is on the blacklist.
     */
    public boolean isTokenBlacklisted(String tokenHash) {
        String key = BLACKLIST_PREFIX + tokenHash;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Increment login attempt counter for an email (rate limiting).
     */
    public long incrementLoginAttempts(String email, Duration window) {
        String key = RATE_LIMIT_PREFIX + email;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, window);
        }
        return Optional.ofNullable(count).orElse(0L);
    }

    /**
     * Get current login attempt count for an email.
     */
    public long getLoginAttempts(String email) {
        String key = RATE_LIMIT_PREFIX + email;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }

    /**
     * Clear login attempt counter (after successful login).
     */
    public void clearLoginAttempts(String email) {
        redisTemplate.delete(RATE_LIMIT_PREFIX + email);
    }
}
