package com.krushna.moviebooking.booking.exception;

import java.util.UUID;

/**
 * Thrown when a user attempts to access or modify a booking owned by another user.
 */
public class InvalidBookingOwnershipException extends RuntimeException {

    public InvalidBookingOwnershipException(UUID bookingId, UUID userId) {
        super(String.format("User %s is not the owner of booking %s", userId, bookingId));
    }

    public InvalidBookingOwnershipException(String bookingReference, UUID userId) {
        super(String.format("User %s is not the owner of booking reference %s", userId, bookingReference));
    }

    public InvalidBookingOwnershipException(String message) {
        super(message);
    }
}
