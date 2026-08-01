package com.krushna.moviebooking.auth.exception;

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

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, "User Not Found", ex.getMessage(), "user-not-found");
    }

    @ExceptionHandler({EmailAlreadyExistsException.class, PhoneNumberAlreadyExistsException.class})
    public ProblemDetail handleConflict(RuntimeException ex) {
        log.warn("Registration conflict: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "Resource Conflict", ex.getMessage(), "registration-conflict");
    }

    @ExceptionHandler({InvalidCredentialsException.class, InvalidTokenException.class, AuthException.class})
    public ProblemDetail handleUnauthorized(RuntimeException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return problem(HttpStatus.UNAUTHORIZED, "Authentication Failed", ex.getMessage(), "auth-failed");
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
