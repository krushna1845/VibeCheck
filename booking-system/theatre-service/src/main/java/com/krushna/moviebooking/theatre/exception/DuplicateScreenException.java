package com.krushna.moviebooking.theatre.exception;

import java.util.UUID;

/**
 * Thrown when attempting to create or update a screen with a name
 * that already exists inside the target theatre.
 */
public class DuplicateScreenException extends RuntimeException {

    public DuplicateScreenException(String name, UUID theatreId) {
        super("A screen with the name '" + name + "' already exists in theatre id: " + theatreId);
    }

    public DuplicateScreenException(String message) {
        super(message);
    }
}
