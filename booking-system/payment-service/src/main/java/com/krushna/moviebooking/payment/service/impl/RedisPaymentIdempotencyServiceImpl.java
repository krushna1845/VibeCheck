package com.krushna.moviebooking.payment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krushna.moviebooking.payment.dto.PaymentResponse;
import com.krushna.moviebooking.payment.service.PaymentIdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Redis-backed implementation of {@link PaymentIdempotencyService}.
 *
 * <p><b>Idempotency key flow</b>:
 * <ol>
 *   <li>On first payment attempt: no Redis entry → proceed with gateway call → cache result.</li>
 *   <li>On retry with same key: Redis hit → return cached response, no gateway call.</li>
 * </ol>
 *
 * <p><b>Callback deduplication flow</b>:
 * <ol>
 *   <li>On first callback: no Redis entry → process and mark as done.</li>
 *   <li>On duplicate callback: Redis hit → skip processing, return existing record.</li>
 * </ol>
 *
 * <p>All Redis operations use atomic Lua scripts or native SET NX to prevent race conditions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPaymentIdempotencyServiceImpl implements PaymentIdempotencyService {

    private static final String IDEMPOTENCY_PREFIX = "payment:idem:";
    private static final String REFUND_IDEMPOTENCY_PREFIX = "payment:refund:idem:";
    private static final String CALLBACK_PREFIX    = "payment:callback:";

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long>   idempotencySetScript;
    private final ObjectMapper        objectMapper;

    @Value("${payment.idempotency.ttl-seconds:86400}")
    private long idempotencyTtlSeconds;

    @Value("${payment.idempotency.callback-ttl-seconds:604800}")
    private long callbackTtlSeconds;

    // -------------------------------------------------------------------------
    // Idempotency key operations
    // -------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public Optional<PaymentResponse> findCachedResponse(String idempotencyKey) {
        String redisKey = IDEMPOTENCY_PREFIX + idempotencyKey;
        String cached = redisTemplate.opsForValue().get(redisKey);
        if (cached == null) {
            log.debug("[Idempotency] Cache miss for key={}", idempotencyKey);
            return Optional.empty();
        }
        try {
            PaymentResponse response = objectMapper.readValue(cached, PaymentResponse.class);
            log.info("[Idempotency] Cache hit for key={} — returning cached response without gateway call", idempotencyKey);
            return Optional.of(response);
        } catch (JsonProcessingException e) {
            log.error("[Idempotency] Failed to deserialize cached response for key={}: {}", idempotencyKey, e.getMessage());
            return Optional.empty();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void cacheResponse(String idempotencyKey, PaymentResponse response) {
        String redisKey = IDEMPOTENCY_PREFIX + idempotencyKey;
        try {
            String json = objectMapper.writeValueAsString(response);
            Long result = redisTemplate.execute(
                    idempotencySetScript,
                    List.of(redisKey),
                    json,
                    String.valueOf(idempotencyTtlSeconds)
            );
            if (Long.valueOf(1L).equals(result)) {
                log.info("[Idempotency] Cached response for key={} ttl={}s", idempotencyKey, idempotencyTtlSeconds);
            } else {
                log.debug("[Idempotency] Key={} already cached — skipping overwrite", idempotencyKey);
            }
        } catch (JsonProcessingException e) {
            log.error("[Idempotency] Failed to serialize response for key={}: {}", idempotencyKey, e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public Optional<com.krushna.moviebooking.payment.dto.RefundResponse> findCachedRefundResponse(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        String redisKey = REFUND_IDEMPOTENCY_PREFIX + idempotencyKey;
        String cached = redisTemplate.opsForValue().get(redisKey);
        if (cached == null) {
            log.debug("[Idempotency] Refund cache miss for key={}", idempotencyKey);
            return Optional.empty();
        }
        try {
            com.krushna.moviebooking.payment.dto.RefundResponse response = objectMapper.readValue(cached, com.krushna.moviebooking.payment.dto.RefundResponse.class);
            log.info("[Idempotency] Refund cache hit for key={} — returning cached refund without gateway call", idempotencyKey);
            return Optional.of(response);
        } catch (JsonProcessingException e) {
            log.error("[Idempotency] Failed to deserialize cached refund for key={}: {}", idempotencyKey, e.getMessage());
            return Optional.empty();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void cacheRefundResponse(String idempotencyKey, com.krushna.moviebooking.payment.dto.RefundResponse response) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || response == null) {
            return;
        }
        String redisKey = REFUND_IDEMPOTENCY_PREFIX + idempotencyKey;
        try {
            String json = objectMapper.writeValueAsString(response);
            Long result = redisTemplate.execute(
                    idempotencySetScript,
                    List.of(redisKey),
                    json,
                    String.valueOf(idempotencyTtlSeconds)
            );
            if (Long.valueOf(1L).equals(result)) {
                log.info("[Idempotency] Cached refund response for key={} ttl={}s", idempotencyKey, idempotencyTtlSeconds);
            } else {
                log.debug("[Idempotency] Refund key={} already cached — skipping overwrite", idempotencyKey);
            }
        } catch (JsonProcessingException e) {
            log.error("[Idempotency] Failed to serialize refund response for key={}: {}", idempotencyKey, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Callback deduplication operations
    // -------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public boolean isCallbackAlreadyProcessed(String transactionReference) {
        String redisKey = CALLBACK_PREFIX + transactionReference;
        Boolean exists = redisTemplate.hasKey(redisKey);
        boolean processed = Boolean.TRUE.equals(exists);
        if (processed) {
            log.warn("[Idempotency] Duplicate callback detected for txnRef={}", transactionReference);
        }
        return processed;
    }

    /** {@inheritDoc} */
    @Override
    public void markCallbackProcessed(String transactionReference) {
        String redisKey = CALLBACK_PREFIX + transactionReference;
        redisTemplate.opsForValue().set(redisKey, "1", Duration.ofSeconds(callbackTtlSeconds));
        log.info("[Idempotency] Marked callback as processed for txnRef={} ttl={}s",
                transactionReference, callbackTtlSeconds);
    }
}
