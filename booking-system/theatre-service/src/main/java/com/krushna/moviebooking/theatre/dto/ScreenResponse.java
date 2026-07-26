package com.krushna.moviebooking.theatre.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Outbound DTO returned from Screen service operations.
 */
@Builder
public record ScreenResponse(
        UUID id,
        UUID theatreId,
        String name,
        String screenType,
        Integer totalSeats,
        Instant createdAt,
        List<SeatResponse> seats
) {}
