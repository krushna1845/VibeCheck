package com.krushna.moviebooking.booking.exception;

import java.util.List;
import java.util.UUID;

/**
 * Thrown when one or more seat IDs do not exist for the specified show.
 */
public class SeatNotFoundException extends RuntimeException {

    public SeatNotFoundException(UUID showId, List<UUID> missingSeatIds) {
        super(String.format("Seats %s do not exist for showId: %s", missingSeatIds, showId));
    }

    public SeatNotFoundException(String message) {
        super(message);
    }
}
