package com.krushna.moviebooking.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Redis configuration for the Payment Service.
 *
 * <p>Provides a {@link StringRedisTemplate} for idempotency key storage and a
 * Lua script for atomic set-if-absent operations used by
 * {@link com.krushna.moviebooking.payment.service.PaymentIdempotencyService}.
 */
@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate paymentStringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    /**
     * Lua script for atomic SET NX (set-if-not-exists) with TTL.
     *
     * <p>KEYS[1]: Redis key (idempotency key)
     * <p>ARGV[1]: Value to store (serialized PaymentResponse JSON)
     * <p>ARGV[2]: TTL in seconds
     * <p>Returns: 1 if key was set (new), 0 if key already existed (duplicate).
     */
    @Bean
    public RedisScript<Long> idempotencySetScript() {
        String script = """
                local existing = redis.call('exists', KEYS[1])
                if existing == 0 then
                    redis.call('set', KEYS[1], ARGV[1], 'EX', tonumber(ARGV[2]))
                    return 1
                end
                return 0
                """;
        return new DefaultRedisScript<>(script, Long.class);
    }
}
