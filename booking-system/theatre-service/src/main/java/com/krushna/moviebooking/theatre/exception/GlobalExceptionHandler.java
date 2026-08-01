package com.krushna.moviebooking.theatre.exception;

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

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({TheatreNotFoundException.class, ScreenNotFoundException.class, SeatNotFoundException.class, CityNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage(), "resource-not-found");
    }

    @ExceptionHandler({DuplicateTheatreException.class, DuplicateScreenException.class, DuplicateSeatException.class})
    public ProblemDetail handleConflict(RuntimeException ex) {
        log.warn("Resource conflict: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "Resource Conflict", ex.getMessage(), "resource-conflict");
    }

    @ExceptionHandler({InactiveTheatreException.class, InvalidSeatTypeException.class, IllegalArgumentException.class})
    public ProblemDetail handleBadRequest(RuntimeException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), "bad-request");
    }

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

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.", "internal-error");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String errorCode) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create("https://moviebooking.com/errors/" + errorCode));
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("errorCode", errorCode);
        return pd;
    }
}
