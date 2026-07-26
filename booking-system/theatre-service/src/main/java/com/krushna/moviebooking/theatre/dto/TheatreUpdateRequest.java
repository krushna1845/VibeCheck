package com.krushna.moviebooking.theatre.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Inbound DTO for updating an existing Theatre (patch semantics).
 */
@Builder
public record TheatreUpdateRequest(

        Integer cityId,

        @Size(max = 150, message = "Theatre name must not exceed 150 characters")
        String name,

        String address,

        BigDecimal latitude,

        BigDecimal longitude,

        String status
) {}
