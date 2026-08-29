package com.krushna.moviebooking.booking.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response object indicating result of seat locking operation.
 *
 * <p>{@code lockToken} is the unique ownership token generated during lock acquisition.
 * Callers MUST persist this token and use it for all subsequent owner-verified release
 * ({@code releaseLocksByToken}) and renewal ({@code renewLocks}) calls.
 */
@Builder
public record SeatLockResponse(
        boolean success,
        UUID showId,
        List<UUID> lockedSeatIds,
        List<UUID> failedSeatIds,
        Instant expiresAt,
        String message,
        /** Unique ownership token for the acquired lock batch. Null when success=false. */
        String lockToken
) {}

