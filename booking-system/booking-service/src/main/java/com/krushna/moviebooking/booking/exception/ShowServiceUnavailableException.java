package com.krushna.moviebooking.booking.exception;

/**
 * Thrown when Show Service is unreachable, times out, or returns a 5xx error.
 */
public class ShowServiceUnavailableException extends RuntimeException {

    public ShowServiceUnavailableException(String message) {
        super(message);
    }

    public ShowServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
