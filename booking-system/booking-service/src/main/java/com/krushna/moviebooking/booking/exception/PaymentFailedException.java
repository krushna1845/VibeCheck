package com.krushna.moviebooking.booking.exception;

/**
 * Thrown when payment processing fails during booking payment.
 */
public class PaymentFailedException extends RuntimeException {

    public PaymentFailedException(String message) {
        super(message);
    }
}
