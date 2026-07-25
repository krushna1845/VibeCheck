package com.krushna.moviebooking.movie.exception;

/**
 * Thrown when an invalid status transition is attempted on a Movie,
 * e.g. trying to set status to an unrecognised value.
 */
public class InvalidMovieStatusException extends RuntimeException {

    public InvalidMovieStatusException(String status) {
        super("Invalid movie status: '" + status + "'. Allowed values are: COMING_SOON, NOW_SHOWING, ENDED.");
    }
}
