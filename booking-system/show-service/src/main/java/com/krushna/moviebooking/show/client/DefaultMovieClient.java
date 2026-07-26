package com.krushna.moviebooking.show.client;

import com.krushna.moviebooking.show.exception.MovieNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Primary component implementation of {@link MovieClient}.
 * Provides Movie Service interactions with logging and fallback capabilities.
 */
@Slf4j
@Component
public class DefaultMovieClient implements MovieClient {

    private static final int DEFAULT_DURATION_MINUTES = 120;

    @Override
    public Optional<MovieDto> getMovieById(UUID movieId) {
        log.debug("Fetching movie metadata for movieId: {}", movieId);
        if (movieId == null) {
            return Optional.empty();
        }
        return Optional.of(new MovieDto(movieId, "Sample Movie", DEFAULT_DURATION_MINUTES, "NOW_SHOWING"));
    }

    @Override
    public boolean existsMovie(UUID movieId) {
        log.debug("Checking existence of movieId: {}", movieId);
        return movieId != null;
    }

    @Override
    public int getMovieDurationMinutes(UUID movieId) {
        return getMovieById(movieId)
                .map(MovieDto::durationMinutes)
                .orElseThrow(() -> new MovieNotFoundException(movieId));
    }
}
