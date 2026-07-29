package com.krushna.moviebooking.booking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Inbound DTO for creating a new movie ticket booking.
 *
 * @param userId      Customer user reference ID
 * @param showId      Show reference ID
 * @param showSeatIds Selected show seat reference IDs
 * @param paymentMethod Preferred payment gateway method (optional)
 */
@Builder
public record BookingRequest(
        @NotNull(message = "User ID is required")
        UUID userId,

        @NotNull(message = "Show ID is required")
        UUID showId,

        @NotEmpty(message = "At least one show seat must be selected")
        List<UUID> showSeatIds,

        String paymentMethod
) {}
