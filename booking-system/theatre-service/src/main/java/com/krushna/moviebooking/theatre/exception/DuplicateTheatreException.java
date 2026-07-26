package com.krushna.moviebooking.theatre.exception;

/**
 * Thrown when attempting to create or update a theatre with a name
 * that already exists in the target city.
 */
public class DuplicateTheatreException extends RuntimeException {

    public DuplicateTheatreException(String name, Integer cityId) {
        super("A theatre with the name '" + name + "' already exists in city id: " + cityId);
    }

    public DuplicateTheatreException(String message) {
        super(message);
    }
}
