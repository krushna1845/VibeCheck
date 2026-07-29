package com.krushna.moviebooking.booking.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response object indicating result of seat locking operation.
 */
@Builder
public record SeatLockResponse(
        boolean success,
        UUID showId,
        List<UUID> lockedSeatIds,
        List<UUID> failedSeatIds,
        Instant expiresAt,
        String message
) {}
