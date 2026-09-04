package com.krushna.moviebooking.booking.idempotency.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krushna.moviebooking.booking.dto.BookingResponse;
import com.krushna.moviebooking.booking.idempotency.BookingIdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed implementation of {@link BookingIdempotencyService}.
 * Caches serialized {@link BookingResponse} under {@code booking:idem:{key}}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisBookingIdempotencyServiceImpl implements BookingIdempotencyService {

    private static final String IDEMPOTENCY_PREFIX = "booking:idem:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${booking.idempotency.ttl-seconds:86400}")
    private long idempotencyTtlSeconds;

    @Override
    public Optional<BookingResponse> findCachedResponse(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        String redisKey = IDEMPOTENCY_PREFIX + idempotencyKey;
        try {
            String cached = redisTemplate.opsForValue().get(redisKey);
            if (cached == null) {
                return Optional.empty();
            }
            BookingResponse response = objectMapper.readValue(cached, BookingResponse.class);
            log.info("[BookingIdempotency] Cache hit for key={} -> returning cached booking {}", idempotencyKey, response.bookingReference());
            return Optional.of(response);
        } catch (Exception e) {
            log.warn("[BookingIdempotency] Failed to read cached booking for key {}: {}", idempotencyKey, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void cacheResponse(String idempotencyKey, BookingResponse response) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || response == null) {
            return;
        }
        String redisKey = IDEMPOTENCY_PREFIX + idempotencyKey;
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(redisKey, json, Duration.ofSeconds(idempotencyTtlSeconds));
            log.info("[BookingIdempotency] Cached booking response for key={} bookingRef={} ttl={}s",
                    idempotencyKey, response.bookingReference(), idempotencyTtlSeconds);
        } catch (JsonProcessingException e) {
            log.error("[BookingIdempotency] Failed to serialize booking response for key {}: {}", idempotencyKey, e.getMessage());
        }
    }
}
