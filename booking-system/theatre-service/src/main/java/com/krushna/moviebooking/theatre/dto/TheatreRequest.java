package com.krushna.moviebooking.theatre.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Inbound DTO for creating a new Theatre.
 */
@Builder
public record TheatreRequest(

        @NotNull(message = "City reference ID is required")
        Integer cityId,

        @NotBlank(message = "Theatre name is required")
        @Size(max = 150, message = "Theatre name must not exceed 150 characters")
        String name,

        @NotBlank(message = "Address is required")
        String address,

        BigDecimal latitude,

        BigDecimal longitude
) {}
