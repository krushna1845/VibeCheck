package com.krushna.moviebooking.payment.gateway;

/**
 * Thrown when the external payment gateway returns an unrecoverable error response
 * (e.g., 4xx client errors from the provider side, authentication failures, invalid card).
 */
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
