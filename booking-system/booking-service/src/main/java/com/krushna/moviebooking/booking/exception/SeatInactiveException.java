package com.krushna.moviebooking.booking.exception;

import java.util.List;
import java.util.UUID;

/**
 * Thrown when one or more seats are disabled, blocked, or inactive for operational reasons.
 */
public class SeatInactiveException extends RuntimeException {

    public SeatInactiveException(UUID showId, List<UUID> inactiveSeatIds) {
        super(String.format("Seats %s are inactive or blocked for showId: %s", inactiveSeatIds, showId));
    }

    public SeatInactiveException(String message) {
        super(message);
    }
}
