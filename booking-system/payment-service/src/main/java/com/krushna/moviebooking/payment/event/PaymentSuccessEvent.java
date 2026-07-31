package com.krushna.moviebooking.payment.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published to Kafka when a gateway callback confirms payment success.
 */
@Builder
public record PaymentSuccessEvent(
        UUID paymentId,
        UUID bookingId,
        String bookingReference,
        UUID userId,
        String transactionReference,
        BigDecimal amount,
        String currency,
        Instant timestamp
) {}
