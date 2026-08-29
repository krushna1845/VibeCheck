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
     * <p><b>WARNING:</b> This overload does NOT verify ownership. It is safe only for
     * compensation paths in acquisition rollback (where we know the current JVM just
     * acquired the lock moments ago with a known token). Do NOT call this from booking
     * lifecycle transitions (confirm, cancel, expire) where another user may have
     * acquired the same seats after the previous lock expired.
     *
     * @param showId Show reference UUID
     * @param seatIds List of show seat UUIDs to release
     */
    void releaseLocks(UUID showId, List<UUID> seatIds);

    /**
     * Atomically releases seat locks for a show only if each lock's stored {@code lockToken}
     * matches the provided token. Seats whose Redis lock is owned by a different token are
     * left untouched and logged as warnings.
     *
     * <p>This is the <b>preferred</b> release method for booking lifecycle transitions
     * (confirmation, cancellation, expiration) where the token was captured at acquisition time.
     *
     * @param showId Show reference UUID
     * @param seatIds List of show seat UUIDs to release
     * @param lockToken The ownership token returned by {@link #lockSeats(SeatLockRequest)}
     */
    void releaseLocksByToken(UUID showId, List<UUID> seatIds, String lockToken);

    /**
     * Atomically releases seat locks for a show only if each lock's stored {@code userId}
     * matches the provided userId. Seats whose Redis lock is owned by a different user are
     * left untouched and logged as warnings.
     *
     * <p>Use this overload when the original {@code lockToken} is not available (e.g.
     * legacy call sites). Prefer {@link #releaseLocksByToken} wherever the token is known.
     *
     * @param showId Show reference UUID
     * @param seatIds List of show seat UUIDs to release
     * @param userId Requesting user UUID (must match the lock owner)
     */
    void releaseLocks(UUID showId, List<UUID> seatIds, UUID userId);

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
