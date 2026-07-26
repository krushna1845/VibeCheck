package com.krushna.moviebooking.show.exception;

import java.util.UUID;

/**
 * Thrown when a referenced Screen cannot be found.
 */
public class ScreenNotFoundException extends RuntimeException {

    public ScreenNotFoundException(UUID id) {
        super("Screen not found with id: " + id);
    }

    public ScreenNotFoundException(String message) {
        super(message);
    }
}
