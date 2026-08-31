package com.krushna.moviebooking.show.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Request payload for updating show seats status.
 */
@Builder
public record SeatStatusUpdateRequest(
        String bookingReference,
        @NotEmpty(message = "showSeatIds must not be empty")
        List<UUID> showSeatIds,
        @NotBlank(message = "status must not be blank")
        String status
) {}
