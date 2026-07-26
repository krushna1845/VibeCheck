package com.krushna.moviebooking.show.exception;

import java.util.UUID;

/**
 * Thrown when a referenced Movie cannot be found.
 */
public class MovieNotFoundException extends RuntimeException {

    public MovieNotFoundException(UUID id) {
        super("Movie not found with id: " + id);
    }

    public MovieNotFoundException(String message) {
        super(message);
    }
}
