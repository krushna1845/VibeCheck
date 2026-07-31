package com.krushna.moviebooking.booking.exception;

import java.util.UUID;

/**
 * Thrown when a user ID does not exist in the system.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID userId) {
        super(String.format("User with ID %s does not exist", userId));
    }

    public UserNotFoundException(String message) {
        super(message);
    }
}
