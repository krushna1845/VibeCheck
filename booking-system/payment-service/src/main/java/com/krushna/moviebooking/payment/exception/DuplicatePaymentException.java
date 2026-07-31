package com.krushna.moviebooking.payment.exception;

/**
 * Thrown when a payment has already been processed and a duplicate initiation is detected
 * outside of the idempotency window (e.g., conflicting idempotency key with different payload).
 */
public class DuplicatePaymentException extends RuntimeException {

    public DuplicatePaymentException(String message) {
        super(message);
    }
}
