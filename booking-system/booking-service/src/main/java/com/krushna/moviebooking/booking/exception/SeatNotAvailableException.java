package com.krushna.moviebooking.booking.exception;

import java.util.List;
import java.util.UUID;

/**
 * Thrown when one or more show seats are not available for booking (already booked or inactive).
 */
public class SeatNotAvailableException extends RuntimeException {

    public SeatNotAvailableException(UUID showId, List<UUID> seatIds) {
        super(String.format("Seats %s are not available for showId: %s", seatIds, showId));
    }
}
