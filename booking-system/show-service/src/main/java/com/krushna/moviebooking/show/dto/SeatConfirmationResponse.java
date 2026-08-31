package com.krushna.moviebooking.show.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Response payload after confirming/updating show seats.
 */
@Builder
public record SeatConfirmationResponse(
        UUID showId,
        String bookingReference,
        List<UUID> confirmedSeatIds,
        String status,
        int count
) {}
