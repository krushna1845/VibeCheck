package com.krushna.moviebooking.booking.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Inbound DTO for updating an existing booking using patch update semantics.
 * All fields are optional. Non-null values will overwrite existing entity fields.
 *
 * @param status          Target booking status (PENDING, CONFIRMED, CANCELLED, EXPIRED)
 * @param totalAmount     Updated total amount
 * @param taxAmount       Updated tax amount
 * @param convenienceFee Updated convenience fee
 * @param expiresAt       Updated reservation expiration timestamp
 */
@Builder
public record BookingUpdateRequest(
        String status,

        @DecimalMin(value = "0.00", message = "Total amount must not be negative")
        BigDecimal totalAmount,

        @DecimalMin(value = "0.00", message = "Tax amount must not be negative")
        BigDecimal taxAmount,

        @DecimalMin(value = "0.00", message = "Convenience fee must not be negative")
        BigDecimal convenienceFee,

        Instant expiresAt
) {}
