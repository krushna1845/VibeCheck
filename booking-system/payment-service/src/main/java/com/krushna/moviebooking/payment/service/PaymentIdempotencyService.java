package com.krushna.moviebooking.payment.service;

import com.krushna.moviebooking.payment.dto.PaymentResponse;

import java.util.Optional;

/**
 * Manages idempotency records for payment initiation requests.
 *
 * <p>Uses Redis to store previously processed responses keyed on caller-supplied
 * idempotency keys. This prevents double-charging on network retries.
 *
 * <p>TTL for cached responses is configurable via {@code payment.idempotency.ttl-seconds}.
 */
public interface PaymentIdempotencyService {

    /**
     * Attempts to retrieve a cached {@link PaymentResponse} for the given idempotency key.
     *
     * @param idempotencyKey Caller-supplied idempotency key
     * @return An {@link Optional} containing the cached response, or empty if not found
     */
    Optional<PaymentResponse> findCachedResponse(String idempotencyKey);

    /**
     * Caches a {@link PaymentResponse} under the given idempotency key.
     *
     * <p>This is a no-op (safe to call multiple times) — subsequent calls with the same key
     * will not overwrite the existing entry due to the atomic SET NX Lua script.
     *
     * @param idempotencyKey Caller-supplied idempotency key
     * @param response       Completed payment response to cache
     */
    void cacheResponse(String idempotencyKey, PaymentResponse response);

    /**
     * Returns {@code true} if a callback for the given transaction reference has already
     * been processed (duplicate callback guard).
     *
     * @param transactionReference External gateway transaction reference
     * @return {@code true} if already processed
     */
    boolean isCallbackAlreadyProcessed(String transactionReference);

    /**
     * Marks a gateway callback transaction reference as processed.
     *
     * @param transactionReference External gateway transaction reference
     */
    void markCallbackProcessed(String transactionReference);
}
