package com.krushna.moviebooking.auth.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(UUID id) {
        super("User not found with id: " + id);
    }
    public UserNotFoundException(String message) {
        super(message);
    }
}
