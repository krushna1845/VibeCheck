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
     * Lua script to atomically release a seat lock ONLY if the stored lock token matches the expected token.
     *
     * <p>Ownership check order:
     * <ol>
     *   <li>Exact string equality: stored value == ARGV[1] (fast path, simple token stored as plain string)</li>
     *   <li>JSON field extraction: stored JSON's {@code lockToken} field == ARGV[1] (normal SeatLock JSON path)</li>
     * </ol>
     *
     * <p>KEYS[1]: Redis lock key (e.g. seat:{showId}:{seatId})
     * <p>ARGV[1]: Lock ownership token (unique UUID generated at lock-acquisition time)
     * <p>Returns: 1 if deleted, 0 if lock not found or owned by someone else.
     */
    @Bean
    public RedisScript<Long> releaseLockScript() {
        String script = """
            local val = redis.call('get', KEYS[1])
            if not val then
                return 0
            end
            if val == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            local ok, decoded = pcall(cjson.decode, val)
            if ok and decoded ~= nil and type(decoded) == 'table' then
                if decoded['lockToken'] == ARGV[1] or decoded['userId'] == ARGV[1] then
                    return redis.call('del', KEYS[1])
                end
            end
            return 0
            """;
        return new DefaultRedisScript<>(script, Long.class);
    }

    /**
     * Lua script to atomically renew TTL for a seat lock ONLY if the stored lock token matches the expected token.
     *
     * <p>Ownership check order:
     * <ol>
     *   <li>Exact string equality: stored value == ARGV[1] (fast path)</li>
     *   <li>JSON field extraction: stored JSON's {@code lockToken} field == ARGV[1] or {@code userId} == ARGV[1]</li>
     * </ol>
     *
     * <p>KEYS[1]: Redis lock key (e.g. seat:{showId}:{seatId})
     * <p>ARGV[1]: Lock ownership token or userId
     * <p>ARGV[2]: New TTL in seconds
     * <p>Returns: 1 if extended, 0 if lock not found or owned by someone else.
     */
    @Bean
    public RedisScript<Long> renewLockScript() {
        String script = """
            local val = redis.call('get', KEYS[1])
            if not val then
                return 0
            end
            if val == ARGV[1] then
                return redis.call('expire', KEYS[1], tonumber(ARGV[2]))
            end
            local ok, decoded = pcall(cjson.decode, val)
            if ok and decoded ~= nil and type(decoded) == 'table' then
                if decoded['lockToken'] == ARGV[1] or decoded['userId'] == ARGV[1] then
                    return redis.call('expire', KEYS[1], tonumber(ARGV[2]))
                end
            end
            return 0
            """;
        return new DefaultRedisScript<>(script, Long.class);
    }
}
