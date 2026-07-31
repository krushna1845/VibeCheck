package com.krushna.moviebooking.payment.exception;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.krushna.moviebooking.payment.gateway.PaymentGatewayException;
import com.krushna.moviebooking.payment.gateway.PaymentTimeoutException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global REST exception handler for the Payment Service.
 *
 * <p>Maps domain and system exceptions to standardised JSON error payloads with
 * appropriate HTTP status codes.
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

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFound(PaymentNotFoundException ex) {
        log.warn("[Payment] Not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "Payment Not Found", ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidPaymentRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidPaymentRequestException ex) {
        log.warn("[Payment] Invalid request: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Invalid Payment Request", ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidSignatureException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSignature(InvalidSignatureException ex) {
        log.warn("[Payment] Signature verification failed: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Signature Verification Failed", ex.getMessage(), null);
    }

    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<ErrorResponse> handleDuplicatePayment(DuplicatePaymentException ex) {
        log.warn("[Payment] Duplicate payment attempt: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "Duplicate Payment", ex.getMessage(), null);
    }

    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<ErrorResponse> handleGatewayError(PaymentGatewayException ex) {
        log.error("[Payment] Gateway error: {}", ex.getMessage());
        return build(HttpStatus.BAD_GATEWAY, "Payment Gateway Error", ex.getMessage(), null);
    }

    @ExceptionHandler(PaymentTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeout(PaymentTimeoutException ex) {
        log.error("[Payment] Gateway timeout: {}", ex.getMessage());
        return build(HttpStatus.GATEWAY_TIMEOUT, "Payment Gateway Timeout", ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        log.warn("[Payment] Validation failed: {}", errors);
        return build(HttpStatus.BAD_REQUEST, "Validation Failed", "One or more fields failed validation", errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("[Payment] Unhandled error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", ex.getMessage(), null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error,
                                                 String message, Map<String, String> validationErrors) {
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
