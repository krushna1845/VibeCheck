package com.krushna.moviebooking.theatre.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Outbound DTO returned from Theatre service operations.
 */
@Builder
public record TheatreResponse(
        UUID id,
        CitySummary city,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String status,
        Instant createdAt,
        Instant updatedAt,
        List<ScreenResponse> screens
) {
    /**
     * Compact summary of associated City entity.
     */
    public record CitySummary(
            Integer id,
            String name,
            String state,
            String country,
            String pincode
    ) {}
}
