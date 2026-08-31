package com.krushna.moviebooking.show.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Request payload for releasing show seats (e.g. on cancellation or expiration).
 */
@Builder
public record SeatReleaseRequest(
        String bookingReference,
        @NotEmpty(message = "showSeatIds must not be empty")
        List<UUID> showSeatIds
) {}
