package com.krushna.moviebooking.movie.exception;

/**
 * Thrown when a Genre with the supplied ID does not exist in the catalogue.
 */
public class GenreNotFoundException extends RuntimeException {

    public GenreNotFoundException(Integer id) {
        super("Genre not found with id: " + id);
    }
}
