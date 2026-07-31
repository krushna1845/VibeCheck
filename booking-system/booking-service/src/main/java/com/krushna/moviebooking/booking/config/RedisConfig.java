package com.krushna.moviebooking.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Spring Configuration for Redis client templates, connection setup, and atomic Lua scripts for distributed seat locking.
 */
@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * Lua script to atomically release a seat lock ONLY if the stored value matches or contains the expected owner token/userId.
     *
     * <p>KEYS[1]: Redis lock key (e.g. seat:{showId}:{seatId})
     * <p>ARGV[1]: Expected owner identifier (e.g. userId string or lock token)
     * <p>Returns: 1 if deleted, 0 if lock not found or owned by someone else.
     */
    @Bean
    public RedisScript<Long> releaseLockScript() {
        String script = """
            local val = redis.call('get', KEYS[1])
            if val then
                if val == ARGV[1] or string.find(val, ARGV[1], 1, true) then
                    return redis.call('del', KEYS[1])
                end
            end
            return 0
            """;
        return new DefaultRedisScript<>(script, Long.class);
    }

    /**
     * Lua script to atomically renew TTL for a seat lock ONLY if the stored value matches or contains the expected owner token/userId.
     *
     * <p>KEYS[1]: Redis lock key (e.g. seat:{showId}:{seatId})
     * <p>ARGV[1]: Expected owner identifier (e.g. userId string or lock token)
     * <p>ARGV[2]: New TTL in seconds
     * <p>Returns: 1 if extended, 0 if lock not found or owned by someone else.
     */
    @Bean
    public RedisScript<Long> renewLockScript() {
        String script = """
            local val = redis.call('get', KEYS[1])
            if val then
                if val == ARGV[1] or string.find(val, ARGV[1], 1, true) then
                    return redis.call('expire', KEYS[1], tonumber(ARGV[2]))
                end
            end
            return 0
            """;
        return new DefaultRedisScript<>(script, Long.class);
    }
}
