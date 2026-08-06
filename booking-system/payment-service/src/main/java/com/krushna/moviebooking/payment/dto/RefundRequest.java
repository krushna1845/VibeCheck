package com.krushna.moviebooking.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record RefundRequest(

        @NotNull(message = "Payment ID is required")
        UUID paymentId,

        @NotNull(message = "Refund amount is required")
        @DecimalMin(value = "0.01", message = "Refund amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Reason for refund is required")
        String reason,

        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey
) {
}
