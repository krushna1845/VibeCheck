package com.krushna.moviebooking.booking.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global REST exception handler mapping domain and system exceptions to RFC 7807 Problem Details payloads.
 */
@Slf4j
@RestControllerAdvice
public class BookingExceptionHandler {

    private static final String TYPE_BASE_URL = "https://api.krushna.com/errors/";

    @ExceptionHandler(BookingNotFoundException.class)
    public ProblemDetail handleBookingNotFound(BookingNotFoundException ex, HttpServletRequest request) {
        log.warn("Booking not found: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.NOT_FOUND, "booking-not-found", "Booking Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler({SeatAlreadyLockedException.class, SeatNotAvailableException.class, SeatUnavailableException.class})
    public ProblemDetail handleSeatConflict(RuntimeException ex, HttpServletRequest request) {
        log.warn("Seat locking/availability conflict: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.CONFLICT, "seat-conflict", "Seat Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidBookingStateException.class)
    public ProblemDetail handleInvalidState(InvalidBookingStateException ex, HttpServletRequest request) {
        log.warn("Invalid booking state: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.BAD_REQUEST, "invalid-booking-state", "Invalid Booking State", ex.getMessage(), request);
    }

    @ExceptionHandler(BookingExpiredException.class)
    public ProblemDetail handleExpired(BookingExpiredException ex, HttpServletRequest request) {
        log.warn("Booking expired: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.GONE, "booking-expired", "Booking Reservation Expired", ex.getMessage(), request);
    }

    @ExceptionHandler({BookingAlreadyCancelledException.class, BookingAlreadyConfirmedException.class})
    public ProblemDetail handleAlreadyProcessed(RuntimeException ex, HttpServletRequest request) {
        log.warn("Booking state error: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.CONFLICT, "booking-state-conflict", "Booking State Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ProblemDetail handlePaymentFailed(PaymentFailedException ex, HttpServletRequest request) {
        log.warn("Payment failed: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.PAYMENT_REQUIRED, "payment-failed", "Payment Failed", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidBookingRequestException.class)
    public ProblemDetail handleInvalidRequest(InvalidBookingRequestException ex, HttpServletRequest request) {
        log.warn("Invalid booking request: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.BAD_REQUEST, "bad-request", "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler({UserNotFoundException.class, ShowNotFoundException.class, SeatNotFoundException.class})
    public ProblemDetail handleResourceNotFound(RuntimeException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.NOT_FOUND, "resource-not-found", "Resource Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler({UserInactiveException.class, UserNotAuthorizedException.class, InvalidBookingOwnershipException.class})
    public ProblemDetail handleAccessDenied(RuntimeException ex, HttpServletRequest request) {
        log.warn("Access denied or user inactive: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.FORBIDDEN, "access-denied", "Access Denied", ex.getMessage(), request);
    }

    @ExceptionHandler({ShowInactiveException.class, ShowExpiredException.class, SeatInactiveException.class})
    public ProblemDetail handleResourceStateError(RuntimeException ex, HttpServletRequest request) {
        log.warn("Resource state error: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.BAD_REQUEST, "invalid-resource-state", "Invalid Resource State", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        log.warn("Validation failed for request: {}", errors);

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "validation-failed",
                "Validation Failed",
                "One or more input fields failed validation checks",
                request
        );
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled internal server error", ex);
        return createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-server-error",
                "Internal Server Error",
                ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred",
                request
        );
    }

    private ProblemDetail createProblemDetail(HttpStatus status, String typeSlug, String title, String detail, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setType(URI.create(TYPE_BASE_URL + typeSlug));
        problemDetail.setTitle(title);
        if (request != null) {
            problemDetail.setInstance(URI.create(request.getRequestURI()));
        }
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}
