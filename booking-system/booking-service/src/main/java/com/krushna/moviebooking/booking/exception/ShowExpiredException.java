package com.krushna.moviebooking.booking.exception;

import java.time.Instant;
import java.util.UUID;

/**
 * Thrown when a show start time has already passed or the show has ended.
 */
public class ShowExpiredException extends RuntimeException {

    public ShowExpiredException(UUID showId, Instant startTime) {
        super(String.format("Show with ID %s has expired (start time: %s)", showId, startTime));
    }

    public ShowExpiredException(String message) {
        super(message);
    }
}
