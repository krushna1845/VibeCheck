package com.krushna.moviebooking.movie.exception;

/**
 * Thrown when an attempt is made to create a Movie whose title already
 * exists in the database (unique constraint guard at the service layer).
 */
public class DuplicateMovieTitleException extends RuntimeException {

    public DuplicateMovieTitleException(String title) {
        super("A movie with the title '" + title + "' already exists.");
    }
}
