package com.krushna.moviebooking.payment.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record RefundResponse(
        UUID refundId,
        UUID paymentId,
        UUID bookingId,
        String refundReference,
        String transactionReference,
        BigDecimal amount,
        String currency,
        String status,
        String reason,
        Instant createdAt
) {
}
