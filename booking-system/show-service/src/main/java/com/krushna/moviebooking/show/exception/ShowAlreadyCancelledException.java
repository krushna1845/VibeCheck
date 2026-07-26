package com.krushna.moviebooking.show.exception;

import java.util.UUID;

/**
 * Thrown when an operation is performed on a Show that has already been cancelled.
 */
public class ShowAlreadyCancelledException extends RuntimeException {

    public ShowAlreadyCancelledException(UUID id) {
        super("Show is already cancelled with id: " + id);
    }

    public ShowAlreadyCancelledException(String message) {
        super(message);
    }
}
