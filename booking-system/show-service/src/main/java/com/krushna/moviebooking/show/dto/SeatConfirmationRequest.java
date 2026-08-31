package com.krushna.moviebooking.show.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Request payload for confirming show seats for a booking.
 */
@Builder
public record SeatConfirmationRequest(
        String bookingReference,
        @NotEmpty(message = "showSeatIds must not be empty")
        List<UUID> showSeatIds
) {}
