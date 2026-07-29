package com.krushna.moviebooking.booking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * Inbound payload from payment gateway or payment service callback.
 */
@Builder
public record PaymentCallbackRequest(
        @NotBlank(message = "Booking reference is required")
        String bookingReference,

        String paymentId,

        @NotBlank(message = "Payment status is required")
        String status,

        String transactionReference,

        String failureReason
) {}
