package com.krushna.moviebooking.theatre.exception;

/**
 * Thrown when an invalid seat category or type is specified.
 */
public class InvalidSeatTypeException extends RuntimeException {

    public InvalidSeatTypeException(String category) {
        super("Invalid seat category/type: '" + category + "'. Allowed values are: REGULAR, PREMIUM, VIP, BALCONY, RECLINER, EXECUTIVE.");
    }

    public InvalidSeatTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
