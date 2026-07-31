package com.krushna.moviebooking.booking.service;

import com.krushna.moviebooking.booking.dto.SeatLockRequest;
import com.krushna.moviebooking.booking.dto.SeatLockResponse;
import com.krushna.moviebooking.booking.model.SeatLock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface managing distributed temporary seat reservations using Redis with TTL expiration.
 *
 * <p>Redis key pattern: {@code seat:{showId}:{seatId}}
 * <p>Supports atomic SETNX lock acquisition, atomic Lua-based release, and atomic Lua-based renewal.
 */
public interface SeatLockService {

    /**
     * Atomically acquires seat locks for a given show and list of seat IDs.
     * If any seat acquisition fails, all previously acquired seats in the batch are rolled back.
     *
     * @param request Seat lock request containing showId, seatIds, userId, bookingReference, and TTL
     * @return SeatLockResponse detailing acquisition success or failure
     */
    SeatLockResponse lockSeats(SeatLockRequest request);

    /**
     * Releases seat locks unconditionally from Redis for a show and list of seat IDs.
     *
     * @param showId Show reference UUID
     * @param seatIds List of show seat UUIDs to release
     */
    void releaseLocks(UUID showId, List<UUID> seatIds);

    /**
     * Atomically releases a single seat lock if owned by the specified user.
     *
     * @param showId Show reference UUID
     * @param seatId Show seat reference UUID
     * @param userId Requesting user UUID
     * @return true if successfully released, false otherwise
     */
    boolean releaseLock(UUID showId, UUID seatId, UUID userId);

    /**
     * Atomically renews the TTL of a single seat lock if owned by the specified user.
     *
     * @param showId Show reference UUID
     * @param seatId Show seat reference UUID
     * @param userId Requesting user UUID
     * @param ttlSeconds TTL duration in seconds
     * @return true if successfully renewed, false if expired or owned by another user
     */
    boolean renewLock(UUID showId, UUID seatId, UUID userId, long ttlSeconds);

    /**
     * Atomically renews the TTL of multiple seat locks if owned by the specified user.
     *
     * @param showId Show reference UUID
     * @param seatIds List of show seat UUIDs
     * @param userId Requesting user UUID
     * @param ttlSeconds TTL duration in seconds
     * @return true if all locks were renewed, false if any failed
     */
    boolean renewLocks(UUID showId, List<UUID> seatIds, UUID userId, long ttlSeconds);

    /**
     * Validates whether a seat lock exists and is owned by the specified user.
     *
     * @param showId Show reference UUID
     * @param seatId Show seat reference UUID
     * @param userId Requesting user UUID
     * @return true if seat lock exists and belongs to userId, false otherwise
     */
    boolean validateOwnership(UUID showId, UUID seatId, UUID userId);

    /**
     * Checks if a seat for a specific show is currently locked in Redis.
     *
     * @param showId Show reference UUID
     * @param seatId Show seat reference UUID
     * @return true if locked, false otherwise
     */
    boolean isSeatLocked(UUID showId, UUID seatId);

    /**
     * Retrieves the seat lock entity for a specific show and seat, if present.
     *
     * @param showId Show reference UUID
     * @param seatId Show seat reference UUID
     * @return Optional containing SeatLock if present, empty otherwise
     */
    Optional<SeatLock> getSeatLock(UUID showId, UUID seatId);
}
