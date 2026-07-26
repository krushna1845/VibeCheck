package com.krushna.moviebooking.show.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Inbound DTO for updating an existing Show's schedule or configuration.
 */
@Builder
public record ShowUpdateRequest(
        UUID movieId,
        UUID theatreId,
        UUID screenId,
        Instant startTime,

        @Size(max = 50, message = "Language must not exceed 50 characters")
        String language,

        String status,
        BigDecimal defaultPrice,
        Map<String, BigDecimal> categoryPrices
) {}
