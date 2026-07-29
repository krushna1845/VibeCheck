package com.krushna.moviebooking.booking.exception;

import java.util.UUID;

/**
 * Thrown when an action cannot be performed because the booking is already cancelled.
 */
public class BookingAlreadyCancelledException extends RuntimeException {

    public BookingAlreadyCancelledException(UUID id) {
        super("Booking is already cancelled with ID: " + id);
    }

    public BookingAlreadyCancelledException(String bookingReference) {
        super("Booking is already cancelled with reference: " + bookingReference);
    }
}
