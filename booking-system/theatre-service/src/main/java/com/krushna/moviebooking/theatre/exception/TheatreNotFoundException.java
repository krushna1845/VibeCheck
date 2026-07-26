package com.krushna.moviebooking.theatre.exception;

import java.util.UUID;

/**
 * Thrown when a Theatre entity cannot be found by the given identifier.
 */
public class TheatreNotFoundException extends RuntimeException {

    public TheatreNotFoundException(UUID id) {
        super("Theatre not found with id: " + id);
    }

    public TheatreNotFoundException(String message) {
        super(message);
    }
}
