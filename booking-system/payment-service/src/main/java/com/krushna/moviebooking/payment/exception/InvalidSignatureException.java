package com.krushna.moviebooking.payment.exception;

/**
 * Thrown when HMAC-SHA256 signature verification of a gateway callback fails.
 * This prevents processing of forged or tampered webhook payloads.
 */
public class InvalidSignatureException extends RuntimeException {

    public InvalidSignatureException(String message) {
        super(message);
    }

    public InvalidSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
