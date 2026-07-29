package com.krushna.moviebooking.booking.exception;

import java.util.List;
import java.util.UUID;

/**
 * Thrown when one or more requested show seats are already locked by another user in Redis.
 */
public class SeatAlreadyLockedException extends RuntimeException {

    public SeatAlreadyLockedException(UUID showId, List<UUID> seatIds) {
        super(String.format("Seats %s are currently locked for showId: %s", seatIds, showId));
    }
}
