package com.krushna.moviebooking.booking.service;

import com.krushna.moviebooking.booking.dto.SeatLockRequest;
import com.krushna.moviebooking.booking.dto.SeatLockResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service interface managing distributed temporary seat reservations using Redis.
 */
public interface SeatLockService {

    /**
     * Atomically acquires seat locks for a given show and list of seat IDs.
     *
     * @param request Seat lock request containing showId, seatIds, userId, and TTL
     * @return SeatLockResponse detailing acquisition success or failure
     */
    SeatLockResponse lockSeats(SeatLockRequest request);

    /**
     * Releases seat locks from Redis for a show and list of seat IDs.
     *
     * @param showId Show reference UUID
     * @param seatIds List of show seat UUIDs to release
     */
    void releaseLocks(UUID showId, List<UUID> seatIds);

    /**
     * Checks if a seat for a specific show is currently locked in Redis.
     *
     * @param showId Show reference UUID
     * @param seatId Show seat reference UUID
     * @return true if locked, false otherwise
     */
    boolean isSeatLocked(UUID showId, UUID seatId);
}
