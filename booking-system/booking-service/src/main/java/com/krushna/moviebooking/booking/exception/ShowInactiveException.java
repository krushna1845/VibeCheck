package com.krushna.moviebooking.booking.exception;

import java.util.UUID;

/**
 * Thrown when a show is not in an active or valid status for booking (e.g. CANCELLED or INACTIVE).
 */
public class ShowInactiveException extends RuntimeException {

    public ShowInactiveException(UUID showId, String status) {
        super(String.format("Show with ID %s is not active (current status: %s)", showId, status));
    }

    public ShowInactiveException(String message) {
        super(message);
    }
}
