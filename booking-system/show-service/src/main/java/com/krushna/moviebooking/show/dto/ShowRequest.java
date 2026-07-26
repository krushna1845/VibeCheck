package com.krushna.moviebooking.show.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Inbound DTO for scheduling a new Show.
 * Validated at controller boundary before service processing.
 */
@Builder
public record ShowRequest(

        @NotNull(message = "Movie ID is required")
        UUID movieId,

        @NotNull(message = "Theatre ID is required")
        UUID theatreId,

        @NotNull(message = "Screen ID is required")
        UUID screenId,

        @NotNull(message = "Start time is required")
        Instant startTime,

        @NotBlank(message = "Language is required")
        @Size(max = 50, message = "Language must not exceed 50 characters")
        String language,

        @NotNull(message = "Default seat price is required")
        @DecimalMin(value = "0.00", message = "Price cannot be negative")
        BigDecimal defaultPrice,

        Map<String, BigDecimal> categoryPrices
) {}
