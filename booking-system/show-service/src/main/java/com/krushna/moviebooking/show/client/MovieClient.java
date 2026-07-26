package com.krushna.moviebooking.show.client;

import java.util.Optional;
import java.util.UUID;

/**
 * Client interface for interacting with the Movie Service.
 */
public interface MovieClient {

    record MovieDto(
            UUID id,
            String title,
            Integer durationMinutes,
            String status
    ) {}

    Optional<MovieDto> getMovieById(UUID movieId);

    boolean existsMovie(UUID movieId);

    int getMovieDurationMinutes(UUID movieId);
}
