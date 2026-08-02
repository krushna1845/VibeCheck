package com.krushna.moviebooking.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentEvents {
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
}
