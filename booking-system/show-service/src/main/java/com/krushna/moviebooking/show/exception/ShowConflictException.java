package com.krushna.moviebooking.show.exception;

import java.time.Instant;
import java.util.UUID;

/**
 * Thrown when a proposed Show time range overlaps with an existing scheduled Show on the same Screen.
 */
public class ShowConflictException extends RuntimeException {

    public ShowConflictException(String message) {
        super(message);
    }

    public ShowConflictException(UUID screenId, Instant startTime, Instant endTime) {
        super(String.format("Show time range [%s - %s] conflicts with an existing show on screen: %s",
                startTime, endTime, screenId));
    }
}
