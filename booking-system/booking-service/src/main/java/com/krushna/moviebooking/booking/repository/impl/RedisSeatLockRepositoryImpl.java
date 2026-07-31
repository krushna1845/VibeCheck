package com.krushna.moviebooking.booking.repository.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.krushna.moviebooking.booking.config.SeatLockProperties;
import com.krushna.moviebooking.booking.model.SeatLock;
import com.krushna.moviebooking.booking.repository.SeatLockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis implementation of {@link SeatLockRepository} using StringRedisTemplate and Lua scripts.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisSeatLockRepositoryImpl implements SeatLockRepository {

    private final StringRedisTemplate stringRedisTemplate;
    private final SeatLockProperties seatLockProperties;
    private final RedisScript<Long> releaseLockScript;
    private final RedisScript<Long> renewLockScript;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public boolean saveIfAbsent(SeatLock seatLock, long ttlSeconds) {
        String key = buildLockKey(seatLock.getShowId(), seatLock.getSeatId());
        String valueJson = serializeSeatLock(seatLock);

        try {
            Boolean success = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, valueJson, Duration.ofSeconds(ttlSeconds));
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.error("Redis error saving lock key: {}", key, e);
            return false;
        }
    }

    @Override
    public Optional<SeatLock> findById(UUID showId, UUID seatId) {
        String key = buildLockKey(showId, seatId);
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(deserializeSeatLock(value, showId, seatId));
        } catch (Exception e) {
            log.error("Redis error fetching lock key: {}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean deleteIfOwnedBy(UUID showId, UUID seatId, String ownerIdentifier) {
        String key = buildLockKey(showId, seatId);
        try {
            Long result = stringRedisTemplate.execute(
                    releaseLockScript,
                    Collections.singletonList(key),
                    ownerIdentifier
            );
            return Long.valueOf(1L).equals(result);
        } catch (Exception e) {
            log.error("Redis error executing release script for key: {}", key, e);
            return false;
        }
    }

    @Override
    public boolean renewIfOwnedBy(UUID showId, UUID seatId, String ownerIdentifier, long ttlSeconds) {
        String key = buildLockKey(showId, seatId);
        try {
            Long result = stringRedisTemplate.execute(
                    renewLockScript,
                    Collections.singletonList(key),
                    ownerIdentifier,
                    String.valueOf(ttlSeconds)
            );
            return Long.valueOf(1L).equals(result);
        } catch (Exception e) {
            log.error("Redis error executing renew script for key: {}", key, e);
            return false;
        }
    }

    @Override
    public boolean exists(UUID showId, UUID seatId) {
        String key = buildLockKey(showId, seatId);
        try {
            Boolean hasKey = stringRedisTemplate.hasKey(key);
            return Boolean.TRUE.equals(hasKey);
        } catch (Exception e) {
            log.error("Redis error checking existence for key: {}", key, e);
            return false;
        }
    }

    @Override
    public void delete(UUID showId, UUID seatId) {
        String key = buildLockKey(showId, seatId);
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Redis error deleting lock key: {}", key, e);
        }
    }

    @Override
    public String buildLockKey(UUID showId, UUID seatId) {
        String prefix = seatLockProperties != null && seatLockProperties.getKeyPrefix() != null
                ? seatLockProperties.getKeyPrefix()
                : "seat:";
        if (!prefix.endsWith(":")) {
            prefix = prefix + ":";
        }
        return String.format("%s%s:%s", prefix, showId, seatId);
    }

    private String serializeSeatLock(SeatLock seatLock) {
        try {
            return objectMapper.writeValueAsString(seatLock);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize SeatLock to JSON, using fallback string. seatLock={}", seatLock, e);
            return String.format("user:%s;token:%s;ref:%s",
                    seatLock.getUserId(), seatLock.getLockToken(), seatLock.getBookingReference());
        }
    }

    private SeatLock deserializeSeatLock(String value, UUID showId, UUID seatId) {
        try {
            if (value.startsWith("{")) {
                SeatLock seatLock = objectMapper.readValue(value, SeatLock.class);
                if (seatLock.getShowId() == null) seatLock.setShowId(showId);
                if (seatLock.getSeatId() == null) seatLock.setSeatId(seatId);
                return seatLock;
            }
        } catch (Exception e) {
            log.debug("Value for key is not JSON, parsing legacy format: {}", value);
        }

        UUID userId = null;
        if (value.contains("user:")) {
            try {
                String uStr = value.substring(value.indexOf("user:") + 5);
                if (uStr.contains(";")) {
                    uStr = uStr.substring(0, uStr.indexOf(";"));
                }
                userId = UUID.fromString(uStr.trim());
            } catch (Exception ignored) {}
        }

        return SeatLock.builder()
                .showId(showId)
                .seatId(seatId)
                .userId(userId)
                .lockToken(value)
                .bookingReference("N/A")
                .build();
    }
}
