package com.krushna.moviebooking.movie.service.impl;

import com.krushna.moviebooking.movie.dto.MovieRequest;
import com.krushna.moviebooking.movie.dto.MovieResponse;
import com.krushna.moviebooking.movie.dto.MovieUpdateRequest;
import com.krushna.moviebooking.movie.entity.Genre;
import com.krushna.moviebooking.movie.entity.Language;
import com.krushna.moviebooking.movie.entity.Movie;
import com.krushna.moviebooking.movie.exception.*;
import com.krushna.moviebooking.movie.mapper.MovieMapper;
import com.krushna.moviebooking.movie.repository.GenreRepository;
import com.krushna.moviebooking.movie.repository.LanguageRepository;
import com.krushna.moviebooking.movie.repository.MovieRepository;
import com.krushna.moviebooking.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Primary implementation of {@link MovieService}.
 *
 * <p><b>Transaction boundaries</b>:
 * <ul>
 *   <li>All write methods are {@code @Transactional} with default propagation
 *       (REQUIRED) — they join an existing TX or start a new one.</li>
 *   <li>All read methods are {@code @Transactional(readOnly = true)} — Hibernate
 *       flushes no dirty-check, and the JDBC driver can route to read replicas.</li>
 * </ul>
 *
 * <p><b>Validation strategy</b>:
 * Bean Validation fires at the controller boundary (via {@code @Valid}).
 * Business rules that require DB lookups (title uniqueness, genre/language
 * existence, soft-delete guard) are enforced here before any write.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private static final Set<String> VALID_STATUSES =
            Set.of("COMING_SOON", "NOW_SHOWING", "ENDED");

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final LanguageRepository languageRepository;
    private final MovieMapper movieMapper;

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Business rules enforced:
     * <ol>
     *   <li>Title must be unique (case-insensitive checked via repo).</li>
     *   <li>Each supplied genre ID must resolve to an existing Genre.</li>
     *   <li>Each supplied language ID must resolve to an existing Language.</li>
     *   <li>Duration > 0 is enforced by Bean Validation on the DTO.</li>
     * </ol>
     */
    @Override
    @Transactional
    public MovieResponse createMovie(MovieRequest request) {
        log.info("Creating movie with title: '{}'", request.title());

        validateUniqueTitleForCreate(request.title());

        Set<Genre> genres = resolveGenres(request.genreIds());
        Set<Language> languages = resolveLanguages(request.languageIds());

        Movie movie = Movie.builder()
                .title(request.title().trim())
                .description(request.description())
                .durationMinutes(request.durationMinutes())
                .releaseDate(request.releaseDate())
                .censorRating(request.censorRating())
                .posterUrl(request.posterUrl())
                .trailerUrl(request.trailerUrl())
                .status("COMING_SOON")
                .genres(genres)
                .languages(languages)
                .build();

        Movie saved = movieRepository.save(movie);
        log.info("Movie created successfully with id: {}", saved.getId());
        return movieMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Business rules enforced:
     * <ol>
     *   <li>Movie must exist.</li>
     *   <li>Movie must not be soft-deleted.</li>
     *   <li>If title is changing, new title must be unique.</li>
     *   <li>If genre IDs are provided, each must resolve.</li>
     *   <li>If language IDs are provided, each must resolve.</li>
     * </ol>
     */
    @Override
    @Transactional
    public MovieResponse updateMovie(UUID id, MovieUpdateRequest request) {
        log.info("Updating movie with id: {}", id);

        Movie movie = findActiveMovieOrThrow(id);

        if (StringUtils.hasText(request.title())
                && !movie.getTitle().equalsIgnoreCase(request.title())) {
            validateUniqueTitleForCreate(request.title());
            movie.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            movie.setDescription(request.description());
        }
        if (request.durationMinutes() != null) {
            movie.setDurationMinutes(request.durationMinutes());
        }
        if (request.releaseDate() != null) {
            movie.setReleaseDate(request.releaseDate());
        }
        if (request.censorRating() != null) {
            movie.setCensorRating(request.censorRating());
        }
        if (request.posterUrl() != null) {
            movie.setPosterUrl(request.posterUrl());
        }
        if (request.trailerUrl() != null) {
            movie.setTrailerUrl(request.trailerUrl());
        }
        if (request.genreIds() != null && !request.genreIds().isEmpty()) {
            movie.setGenres(resolveGenres(request.genreIds()));
        }
        if (request.languageIds() != null && !request.languageIds().isEmpty()) {
            movie.setLanguages(resolveLanguages(request.languageIds()));
        }

        // Dirty checking — no explicit save() needed; Hibernate detects changes.
        log.info("Movie updated successfully: id={}", id);
        return movieMapper.toResponse(movie);
    }

    // -------------------------------------------------------------------------
    // DELETE (soft)
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Issues a soft-delete only — sets {@code deletedAt} to the current
     * instant. No physical row is removed, preserving audit history.
     */
    @Override
    @Transactional
    public void deleteMovie(UUID id) {
        log.info("Soft-deleting movie with id: {}", id);

        Movie movie = findActiveMovieOrThrow(id);
        movie.setDeletedAt(Instant.now());

        log.info("Movie soft-deleted successfully: id={}", id);
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public MovieResponse getMovieById(UUID id) {
        log.debug("Fetching movie by id: {}", id);

        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException(id));

        return movieMapper.toResponse(movie);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Page<MovieResponse> getAllMovies(Pageable pageable) {
        log.debug("Fetching all movies, page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        return movieRepository.findAll(pageable)
                .map(movieMapper::toResponse);
    }

    /**
     * {@inheritDoc}
     *
     * <p>When {@code status} is non-null it is validated against the known
     * state set before being used as a filter, producing a clear error message
     * rather than an empty result-set.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MovieResponse> searchMovies(String keyword, String status, Pageable pageable) {
        log.debug("Searching movies: keyword='{}', status='{}'", keyword, status);

        if (StringUtils.hasText(status)) {
            validateStatus(status);
            return movieRepository.findByTitleContainingIgnoreCaseAndStatus(keyword, status, pageable)
                    .map(movieMapper::toResponse);
        }

        return movieRepository.findByTitleContainingIgnoreCase(keyword, pageable)
                .map(movieMapper::toResponse);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The status transition is validated against the allowed state set.
     * Soft-deleted movies cannot have their status changed.
     */
    @Override
    @Transactional
    public MovieResponse changeMovieStatus(UUID id, String status) {
        log.info("Changing status of movie id={} to '{}'", id, status);

        validateStatus(status);

        Movie movie = findActiveMovieOrThrow(id);
        movie.setStatus(status);

        log.info("Movie id={} status changed to '{}'", id, status);
        return movieMapper.toResponse(movie);
    }

    // -------------------------------------------------------------------------
    // Private validation helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves a set of Genre IDs to Genre entities.
     * Throws {@link GenreNotFoundException} for the first ID that cannot be found.
     */
    private Set<Genre> resolveGenres(Set<Integer> genreIds) {
        Set<Genre> genres = new HashSet<>();
        for (Integer genreId : genreIds) {
            Genre genre = genreRepository.findById(genreId)
                    .orElseThrow(() -> new GenreNotFoundException(genreId));
            genres.add(genre);
        }
        return genres;
    }

    /**
     * Resolves a set of Language IDs to Language entities.
     * Throws {@link LanguageNotFoundException} for the first ID that cannot be found.
     */
    private Set<Language> resolveLanguages(Set<Integer> languageIds) {
        Set<Language> languages = new HashSet<>();
        for (Integer languageId : languageIds) {
            Language language = languageRepository.findById(languageId)
                    .orElseThrow(() -> new LanguageNotFoundException(languageId));
            languages.add(language);
        }
        return languages;
    }

    /**
     * Checks that no active (non-deleted) movie with the same title exists.
     * Title comparison is case-insensitive to prevent near-duplicate entries.
     */
    private void validateUniqueTitleForCreate(String title) {
        movieRepository.findByTitleContainingIgnoreCase(title, Pageable.unpaged())
                .stream()
                .filter(m -> m.getTitle().equalsIgnoreCase(title.trim()))
                .filter(m -> m.getDeletedAt() == null)
                .findFirst()
                .ifPresent(m -> {
                    throw new DuplicateMovieTitleException(title);
                });
    }

    /**
     * Loads an active (non-deleted) movie or throws appropriate domain exceptions.
     * Used as a single entry point for all write/read-write operations to ensure
     * consistent error semantics.
     */
    private Movie findActiveMovieOrThrow(UUID id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException(id));

        if (movie.getDeletedAt() != null) {
            throw new MovieAlreadyDeletedException(id);
        }
        return movie;
    }

    /**
     * Validates a status string against the domain-allowed set.
     */
    private void validateStatus(String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new InvalidMovieStatusException(status);
        }
    }
}
