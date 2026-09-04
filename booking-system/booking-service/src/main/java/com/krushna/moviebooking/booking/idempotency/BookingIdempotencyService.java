package com.krushna.moviebooking.booking.idempotency;

import com.krushna.moviebooking.booking.dto.BookingResponse;

import java.util.Optional;

/**
 * Manages idempotency records for customer booking creation requests.
 * Prevents duplicate seat locking and multiple booking creations on network retries.
 */
public interface BookingIdempotencyService {

    /**
     * Attempts to retrieve a cached {@link BookingResponse} for the given idempotency key.
     *
     * @param idempotencyKey Caller-supplied idempotency key
     * @return An {@link Optional} containing the cached response, or empty if not found
     */
    Optional<BookingResponse> findCachedResponse(String idempotencyKey);

    /**
     * Caches a {@link BookingResponse} under the given idempotency key.
     *
     * @param idempotencyKey Caller-supplied idempotency key
     * @param response Completed booking response to cache
     */
    void cacheResponse(String idempotencyKey, BookingResponse response);
}
