package com.krushna.moviebooking.show.exception;

import java.util.UUID;

/**
 * Thrown when an update or cancellation is attempted on a Show that has already started.
 */
public class ShowAlreadyStartedException extends RuntimeException {

    public ShowAlreadyStartedException(UUID id) {
        super("Show has already started and cannot be modified or cancelled, id: " + id);
    }

    public ShowAlreadyStartedException(String message) {
        super(message);
    }
}
