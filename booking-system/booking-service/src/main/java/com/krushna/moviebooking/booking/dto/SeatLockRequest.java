package com.krushna.moviebooking.booking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Request object for Redis seat locking operation.
 */
@Builder
public record SeatLockRequest(
        @NotNull(message = "Show ID is required")
        UUID showId,

        @NotEmpty(message = "Seat IDs cannot be empty")
        List<UUID> seatIds,

        @NotNull(message = "User ID is required")
        UUID userId,

        String bookingReference,

        long ttlSeconds
) {}
