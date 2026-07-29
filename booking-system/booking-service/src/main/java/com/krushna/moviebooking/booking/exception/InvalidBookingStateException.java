package com.krushna.moviebooking.booking.exception;

/**
 * Thrown when an illegal state transition is attempted on a Booking.
 */
public class InvalidBookingStateException extends RuntimeException {

    public InvalidBookingStateException(String message) {
        super(message);
    }
}
