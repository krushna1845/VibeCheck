package com.krushna.moviebooking.payment.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published to Kafka when a payment refund is processed.
 */
@Builder
public record PaymentRefundedEvent(
        UUID refundId,
        UUID paymentId,
        UUID bookingId,
        UUID userId,
        String refundReference,
        String transactionReference,
        BigDecimal amount,
        String currency,
        String reason,
        Instant timestamp
) {
}
