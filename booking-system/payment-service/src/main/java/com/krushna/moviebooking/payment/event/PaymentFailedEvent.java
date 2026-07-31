package com.krushna.moviebooking.payment.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published to Kafka when a gateway callback reports payment failure.
 */
@Builder
public record PaymentFailedEvent(
        UUID paymentId,
        UUID bookingId,
        String bookingReference,
        UUID userId,
        String transactionReference,
        BigDecimal amount,
        String currency,
        String failureReason,
        Instant timestamp
) {}
