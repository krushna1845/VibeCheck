package com.krushna.moviebooking.booking.exception;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global REST exception handler mapping domain and system exceptions to standardized JSON payloads.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Builder
    public record ErrorResponse(
            int status,
            String error,
            String message,
            Instant timestamp,
            Map<String, String> validationErrors
    ) {}

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookingNotFound(BookingNotFoundException ex) {
        log.warn("Booking not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "Booking Not Found", ex.getMessage(), null);
    }

    @ExceptionHandler(SeatAlreadyLockedException.class)
    public ResponseEntity<ErrorResponse> handleSeatAlreadyLocked(SeatAlreadyLockedException ex) {
        log.warn("Seat locking conflict: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "Seat Lock Conflict", ex.getMessage(), null);
    }

    @ExceptionHandler(SeatNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleSeatNotAvailable(SeatNotAvailableException ex) {
        log.warn("Seat not available: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "Seat Not Available", ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidBookingStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(InvalidBookingStateException ex) {
        log.warn("Invalid booking state: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid Booking State", ex.getMessage(), null);
    }

    @ExceptionHandler(BookingExpiredException.class)
    public ResponseEntity<ErrorResponse> handleExpired(BookingExpiredException ex) {
        log.warn("Booking expired: {}", ex.getMessage());
        return buildResponse(HttpStatus.GONE, "Booking Reservation Expired", ex.getMessage(), null);
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentFailed(PaymentFailedException ex) {
        log.warn("Payment failed: {}", ex.getMessage());
        return buildResponse(HttpStatus.PAYMENT_REQUIRED, "Payment Failed", ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidBookingRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidBookingRequestException ex) {
        log.warn("Invalid booking request: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        log.warn("Validation failed: {}", errors);
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Failed", "One or more fields failed validation", errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled internal server error", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", ex.getMessage(), null);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String error, String message, Map<String, String> validationErrors) {
        ErrorResponse body = ErrorResponse.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .timestamp(Instant.now())
                .validationErrors(validationErrors)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
