package com.krushna.moviebooking.booking.exception;

import java.util.List;
import java.util.UUID;

/**
 * Thrown when one or more requested show seats are not available for booking
 * (either already booked, inactive, or locked by another transaction).
 */
public class SeatUnavailableException extends RuntimeException {

    public SeatUnavailableException(UUID showId, List<UUID> seatIds) {
        super(String.format("Seats %s are unavailable for showId: %s", seatIds, showId));
    }

    public SeatUnavailableException(String message) {
        super(message);
    }
}
