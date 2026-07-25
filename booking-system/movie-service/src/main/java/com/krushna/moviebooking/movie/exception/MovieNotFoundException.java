package com.krushna.moviebooking.movie.exception;

import java.util.UUID;

/**
 * Thrown when a Movie entity cannot be found by the given identifier.
 */
public class MovieNotFoundException extends RuntimeException {

    public MovieNotFoundException(UUID id) {
        super("Movie not found with id: " + id);
    }

    public MovieNotFoundException(String message) {
        super(message);
    }
}
