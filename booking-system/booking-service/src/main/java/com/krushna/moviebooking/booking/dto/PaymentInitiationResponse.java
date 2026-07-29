package com.krushna.moviebooking.booking.dto;

import lombok.Builder;

import java.util.UUID;

/**
 * Response payload received from Payment Service after payment initiation.
 */
@Builder
public record PaymentInitiationResponse(
        UUID paymentId,
        String bookingReference,
        String paymentStatus,
        String redirectUrl,
        String transactionReference
) {}
