package com.krushna.moviebooking.payment.exception;

/**
 * Thrown when a payment cannot be found by the given identifier.
 */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String message) {
        super(message);
    }
}
