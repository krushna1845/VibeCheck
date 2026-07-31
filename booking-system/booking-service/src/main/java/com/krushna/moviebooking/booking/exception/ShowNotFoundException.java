package com.krushna.moviebooking.booking.exception;

import java.util.UUID;

/**
 * Thrown when a show reference ID does not exist.
 */
public class ShowNotFoundException extends RuntimeException {

    public ShowNotFoundException(UUID showId) {
        super(String.format("Show with ID %s does not exist", showId));
    }

    public ShowNotFoundException(String message) {
        super(message);
    }
}
