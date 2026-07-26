package com.krushna.moviebooking.theatre.exception;

import java.util.UUID;

/**
 * Thrown when attempting to create a seat with a row and number
 * that already exists inside the target screen.
 */
public class DuplicateSeatException extends RuntimeException {

    public DuplicateSeatException(String seatRow, Integer seatNumber, UUID screenId) {
        super("A seat with row '" + seatRow + "' and number " + seatNumber + " already exists in screen id: " + screenId);
    }

    public DuplicateSeatException(String message) {
        super(message);
    }
}
