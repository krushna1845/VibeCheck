package com.krushna.moviebooking.theatre.exception;

import java.util.UUID;

/**
 * Thrown when a Screen entity cannot be found by the given identifier.
 */
public class ScreenNotFoundException extends RuntimeException {

    public ScreenNotFoundException(UUID id) {
        super("Screen not found with id: " + id);
    }

    public ScreenNotFoundException(String message) {
        super(message);
    }
}
