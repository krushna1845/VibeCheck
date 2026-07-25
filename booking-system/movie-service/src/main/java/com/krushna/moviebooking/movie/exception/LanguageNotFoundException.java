package com.krushna.moviebooking.movie.exception;

/**
 * Thrown when a Language with the supplied ID does not exist in the catalogue.
 */
public class LanguageNotFoundException extends RuntimeException {

    public LanguageNotFoundException(Integer id) {
        super("Language not found with id: " + id);
    }
}
