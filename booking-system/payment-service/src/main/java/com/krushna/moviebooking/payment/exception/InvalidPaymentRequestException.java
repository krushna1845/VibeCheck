package com.krushna.moviebooking.payment.exception;

/**
 * Thrown when a payment request fails business-level validation
 * (e.g., invalid amount, unsupported currency, blank idempotency key).
 */
public class InvalidPaymentRequestException extends RuntimeException {

    public InvalidPaymentRequestException(String message) {
        super(message);
    }
}
