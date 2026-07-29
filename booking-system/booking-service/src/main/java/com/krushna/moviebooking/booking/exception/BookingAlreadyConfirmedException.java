package com.krushna.moviebooking.booking.exception;

import java.util.UUID;

/**
 * Thrown when an action cannot be performed because the booking is already confirmed.
 */
public class BookingAlreadyConfirmedException extends RuntimeException {

    public BookingAlreadyConfirmedException(UUID id) {
        super("Booking is already confirmed with ID: " + id);
    }

    public BookingAlreadyConfirmedException(String bookingReference) {
        super("Booking is already confirmed with reference: " + bookingReference);
    }
}
