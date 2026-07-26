package com.krushna.moviebooking.show.exception;

import java.util.UUID;

/**
 * Thrown when a Show entity cannot be found by the given identifier.
 */
public class ShowNotFoundException extends RuntimeException {

    public ShowNotFoundException(UUID id) {
        super("Show not found with id: " + id);
    }

    public ShowNotFoundException(String message) {
        super(message);
    }
}
