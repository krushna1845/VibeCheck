package com.krushna.moviebooking.booking.repository;

import com.krushna.moviebooking.booking.model.SeatLock;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing distributed {@link SeatLock} operations in Redis.
 */
public interface SeatLockRepository {

    /**
     * Atomically saves a seat lock in Redis if absent (SETNX).
     */
    boolean saveIfAbsent(SeatLock seatLock, long ttlSeconds);

    /**
     * Finds a seat lock by showId and seatId.
     */
    Optional<SeatLock> findById(UUID showId, UUID seatId);

    /**
     * Atomically releases a seat lock if owned by the specified owner identifier.
     */
    boolean deleteIfOwnedBy(UUID showId, UUID seatId, String ownerIdentifier);

    /**
     * Atomically renews TTL for a seat lock if owned by the specified owner identifier.
     */
    boolean renewIfOwnedBy(UUID showId, UUID seatId, String ownerIdentifier, long ttlSeconds);

    /**
     * Checks if a seat lock key exists in Redis.
     */
    boolean exists(UUID showId, UUID seatId);

    /**
     * Unconditionally deletes a seat lock key from Redis.
     */
    void delete(UUID showId, UUID seatId);

    /**
     * Formats Redis key for given showId and seatId.
     */
    String buildLockKey(UUID showId, UUID seatId);
}
