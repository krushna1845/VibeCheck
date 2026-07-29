package com.krushna.moviebooking.booking.exception;

/**
 * Thrown when a booking request contains invalid parameters or fails domain validation.
 */
public class InvalidBookingRequestException extends RuntimeException {

    public InvalidBookingRequestException(String message) {
        super(message);
    }
}
