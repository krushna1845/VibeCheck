package com.krushna.moviebooking.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Inbound DTO for initiating a payment request.
 *
 * <p>The {@code idempotencyKey} is mandatory and uniquely identifies this payment
 * attempt; duplicate keys with an identical request will return the cached result.
 *
 * @param bookingId       The booking UUID this payment is for
 * @param userId          Paying customer UUID
 * @param idempotencyKey  Caller-supplied unique key to enable idempotent retries
 * @param amount          Payment amount in the given currency
 * @param currency        ISO-4217 currency code (default: INR)
 * @param paymentMethod   Payment method to use (e.g. UPI, CARD, NETBANKING)
 * @param bookingReference 12-character human-readable booking reference
 */
@Builder
public record PaymentRequest(

        @NotNull(message = "Booking ID is required")
        UUID bookingId,

        @NotNull(message = "User ID is required")
        UUID userId,

        @NotBlank(message = "Idempotency key is required")
        @Size(max = 255, message = "Idempotency key must be at most 255 characters")
        String idempotencyKey,

        @NotNull(message = "Payment amount is required")
        @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
        BigDecimal amount,

        @Size(max = 3)
        String currency,

        @Size(max = 50)
        String paymentMethod,

        @NotBlank(message = "Booking reference is required")
        @Size(max = 12)
        String bookingReference
) {}
