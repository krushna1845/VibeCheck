package com.krushna.moviebooking.movie.exception;

import java.util.UUID;

/**
 * Thrown when an update is attempted on a soft-deleted Movie.
 */
public class MovieAlreadyDeletedException extends RuntimeException {

    public MovieAlreadyDeletedException(UUID id) {
        super("Movie with id '" + id + "' has been deleted and cannot be modified.");
    }
}
