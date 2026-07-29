package com.krushna.moviebooking.booking.exception;

import java.util.UUID;

/**
 * Thrown when trying to confirm or process a booking that has passed its expiration time.
 */
public class BookingExpiredException extends RuntimeException {

    public BookingExpiredException(UUID id) {
        super("Booking reservation has expired for ID: " + id);
    }

    public BookingExpiredException(String bookingReference) {
        super("Booking reservation has expired for reference: " + bookingReference);
    }
}
