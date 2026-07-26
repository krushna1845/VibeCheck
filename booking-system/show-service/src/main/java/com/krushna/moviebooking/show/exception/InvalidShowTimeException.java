package com.krushna.moviebooking.show.exception;

/**
 * Thrown when the specified Show start time or end time fails validation rules.
 */
public class InvalidShowTimeException extends RuntimeException {

    public InvalidShowTimeException(String message) {
        super(message);
    }
}
