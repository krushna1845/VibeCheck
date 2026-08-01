package com.krushna.moviebooking.movie.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler for movie-service.
 *
 * All responses use RFC 9457 ProblemDetail format for API consistency.
 * This avoids generic 500 errors leaking internal stack traces to clients.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -------------------------------------------------------------------------
    // Domain Exceptions — 404
    // -------------------------------------------------------------------------

    @ExceptionHandler(MovieNotFoundException.class)
    public ProblemDetail handleMovieNotFound(MovieNotFoundException ex) {
        log.warn("Movie not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Movie Not Found", ex.getMessage(), "movie-not-found");
    }

    @ExceptionHandler(GenreNotFoundException.class)
    public ProblemDetail handleGenreNotFound(GenreNotFoundException ex) {
        log.warn("Genre not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Genre Not Found", ex.getMessage(), "genre-not-found");
    }

    @ExceptionHandler(LanguageNotFoundException.class)
    public ProblemDetail handleLanguageNotFound(LanguageNotFoundException ex) {
        log.warn("Language not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Language Not Found", ex.getMessage(), "language-not-found");
    }

    // -------------------------------------------------------------------------
    // Domain Exceptions — 409 Conflict
    // -------------------------------------------------------------------------

    @ExceptionHandler(DuplicateMovieTitleException.class)
    public ProblemDetail handleDuplicateTitle(DuplicateMovieTitleException ex) {
        log.warn("Duplicate movie title: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "Duplicate Movie Title", ex.getMessage(), "duplicate-movie-title");
    }

    // -------------------------------------------------------------------------
    // Domain Exceptions — 400 Bad Request
    // -------------------------------------------------------------------------

    @ExceptionHandler(InvalidMovieStatusException.class)
    public ProblemDetail handleInvalidStatus(InvalidMovieStatusException ex) {
        log.warn("Invalid movie status: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Invalid Movie Status", ex.getMessage(), "invalid-movie-status");
    }

    @ExceptionHandler(MovieAlreadyDeletedException.class)
    public ProblemDetail handleAlreadyDeleted(MovieAlreadyDeletedException ex) {
        log.warn("Movie already deleted: {}", ex.getMessage());
        return problem(HttpStatus.GONE, "Movie Already Deleted", ex.getMessage(), "movie-already-deleted");
    }

    // -------------------------------------------------------------------------
    // Bean Validation — 400
    // -------------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {} field errors", ex.getBindingResult().getFieldErrorCount());

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Validation Failed",
                "Request validation failed. See 'fieldErrors' for details.", "validation-failed");
        pd.setProperty("fieldErrors", fieldErrors);
        return pd;
    }

    // -------------------------------------------------------------------------
    // Catch-all — 500
    // -------------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.", "internal-error");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ProblemDetail problem(HttpStatus status, String title, String detail, String errorCode) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create("https://moviebooking.com/errors/" + errorCode));
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("errorCode", errorCode);
        return pd;
    }
}
