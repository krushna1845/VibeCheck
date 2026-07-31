package com.krushna.moviebooking.booking.exception;

import java.util.UUID;

/**
 * Thrown when a user account is suspended, deactivated, or inactive.
 */
public class UserInactiveException extends RuntimeException {

    public UserInactiveException(UUID userId) {
        super(String.format("User with ID %s is not active", userId));
    }

    public UserInactiveException(String message) {
        super(message);
    }
}
