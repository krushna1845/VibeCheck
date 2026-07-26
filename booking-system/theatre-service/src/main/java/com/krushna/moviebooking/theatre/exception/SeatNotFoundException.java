package com.krushna.moviebooking.theatre.exception;

import java.util.UUID;

/**
 * Thrown when a Seat entity cannot be found by the given identifier.
 */
public class SeatNotFoundException extends RuntimeException {

    public SeatNotFoundException(UUID id) {
        super("Seat not found with id: " + id);
    }

    public SeatNotFoundException(String message) {
        super(message);
    }
}
