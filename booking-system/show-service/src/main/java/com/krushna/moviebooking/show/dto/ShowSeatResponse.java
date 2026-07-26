package com.krushna.moviebooking.show.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Outbound DTO representing a ShowSeat's status and pricing.
 */
@Builder
public record ShowSeatResponse(
        UUID id,
        UUID showId,
        UUID seatId,
        BigDecimal price,
        String status,
        Instant lockExpiration,
        Long version
) {}
