package com.krushna.moviebooking.theatre.exception;

/**
 * Thrown when a City entity cannot be found by the given identifier or name.
 */
public class CityNotFoundException extends RuntimeException {

    public CityNotFoundException(Integer id) {
        super("City not found with id: " + id);
    }

    public CityNotFoundException(String name, String state) {
        super("City not found with name '" + name + "' and state '" + state + "'");
    }

    public CityNotFoundException(String message) {
        super(message);
    }
}
