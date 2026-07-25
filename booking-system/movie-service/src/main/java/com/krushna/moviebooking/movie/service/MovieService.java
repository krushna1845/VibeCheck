package com.krushna.moviebooking.movie.service;

import com.krushna.moviebooking.movie.dto.MovieRequest;
import com.krushna.moviebooking.movie.dto.MovieResponse;
import com.krushna.moviebooking.movie.dto.MovieUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Contract for all Movie domain operations.
 *
 * Separating the interface from the implementation:
 *  - Enables easy mocking in unit tests.
 *  - Supports future facade / proxy decorators (caching, metrics).
 *  - Enforces dependency inversion — callers depend on abstraction.
 */
public interface MovieService {

    /**
     * Creates a new Movie from the given request after performing
     * uniqueness, genre, and language existence validation.
     *
     * @param request the inbound create payload
     * @return the persisted movie as a response DTO
     */
    MovieResponse createMovie(MovieRequest request);

    /**
     * Applies patch-semantics update to an existing Movie.
     * Only non-null fields in the request are applied.
     * Throws {@link com.krushna.moviebooking.movie.exception.MovieAlreadyDeletedException}
     * if the movie has been soft-deleted.
     *
     * @param id      the movie UUID to update
     * @param request the inbound update payload
     * @return the updated movie as a response DTO
     */
    MovieResponse updateMovie(UUID id, MovieUpdateRequest request);

    /**
     * Soft-deletes a Movie by setting its {@code deletedAt} timestamp.
     * Does not issue a physical DELETE.
     *
     * @param id the movie UUID to delete
     */
    void deleteMovie(UUID id);

    /**
     * Retrieves a single Movie by primary key.
     *
     * @param id the movie UUID
     * @return the movie as a response DTO
     */
    MovieResponse getMovieById(UUID id);

    /**
     * Returns a paginated list of all non-deleted Movies.
     *
     * @param pageable pagination and sort parameters
     * @return a page of movie response DTOs
     */
    Page<MovieResponse> getAllMovies(Pageable pageable);

    /**
     * Searches Movies by title keyword (case-insensitive partial match)
     * and optionally filtered by status.
     *
     * @param keyword  the search term
     * @param status   optional status filter (e.g. "NOW_SHOWING"); null = all
     * @param pageable pagination and sort parameters
     * @return a page of matching movie response DTOs
     */
    Page<MovieResponse> searchMovies(String keyword, String status, Pageable pageable);

    /**
     * Transitions the Movie to the given status after validating
     * the target status value against the allowed state machine.
     *
     * @param id     the movie UUID
     * @param status the target status string
     * @return the updated movie as a response DTO
     */
    MovieResponse changeMovieStatus(UUID id, String status);
}
