package com.krushna.moviebooking.booking.exception;

import java.util.UUID;

/**
 * Thrown when a requested booking is not found by ID or booking reference.
 */
public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(UUID id) {
        super("Booking not found with ID: " + id);
    }

    public BookingNotFoundException(String bookingReference) {
        super("Booking not found with reference: " + bookingReference);
    }
}
