package com.krushna.moviebooking.payment.gateway;

/**
 * Thrown when the payment gateway does not respond within the configured timeout window.
 * This exception should generally be treated as retryable.
 */
public class PaymentTimeoutException extends RuntimeException {

    public PaymentTimeoutException(String message) {
        super(message);
    }

    public PaymentTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
