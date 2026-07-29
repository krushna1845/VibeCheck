package com.krushna.moviebooking.booking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request payload sent to Payment Service to initiate a payment transaction.
 */
@Builder
public record PaymentInitiationRequest(
        @NotBlank(message = "Booking reference is required")
        String bookingReference,

        @NotNull(message = "User ID is required")
        UUID userId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        BigDecimal amount,

        @NotBlank(message = "Payment method is required")
        String paymentMethod
) {}
