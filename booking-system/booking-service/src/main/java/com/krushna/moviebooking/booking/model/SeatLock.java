package com.krushna.moviebooking.booking.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain entity representing a distributed temporary seat reservation lock in Redis.
 *
 * <p>Serialized to JSON and stored under key format: {@code seat:{showId}:{seatId}}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatLock {

    private UUID showId;
    private UUID seatId;
    private UUID userId;
    private String lockToken;
    private String bookingReference;
    private Instant lockedAt;
    private Instant expiresAt;
    private long ttlSeconds;

    /**
     * Checks whether the lock is currently expired based on timestamp.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}
