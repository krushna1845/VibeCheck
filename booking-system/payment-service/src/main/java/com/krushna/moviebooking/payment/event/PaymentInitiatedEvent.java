package com.krushna.moviebooking.payment.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published to Kafka when a payment is successfully initiated.
 */
@Builder
public record PaymentInitiatedEvent(
        UUID paymentId,
        UUID bookingId,
        String bookingReference,
        UUID userId,
        String idempotencyKey,
        String transactionReference,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        Instant timestamp
) {}
