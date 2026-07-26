package com.krushna.moviebooking.theatre.exception;

import java.util.UUID;

/**
 * Thrown when an operation is performed on a theatre that is inactive or soft-deleted.
 */
public class InactiveTheatreException extends RuntimeException {

    public InactiveTheatreException(UUID theatreId) {
        super("Theatre with id " + theatreId + " is inactive or soft-deleted.");
    }

    public InactiveTheatreException(String message) {
        super(message);
    }
}
