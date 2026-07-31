package com.krushna.moviebooking.booking.exception;

import java.util.UUID;

/**
 * Thrown when a user lacks required permissions or authorization to perform an operation.
 */
public class UserNotAuthorizedException extends RuntimeException {

    public UserNotAuthorizedException(UUID userId, String reason) {
        super(String.format("User %s is not authorized: %s", userId, reason));
    }

    public UserNotAuthorizedException(String message) {
        super(message);
    }
}
