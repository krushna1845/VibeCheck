package com.krushna.moviebooking.show.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Outbound DTO returned from Show Service operations.
 */
@Builder
public record ShowResponse(
        UUID id,
        UUID movieId,
        UUID theatreId,
        UUID screenId,
        Instant startTime,
        Instant endTime,
        String language,
        String status,
        Instant createdAt,
        Instant updatedAt,
        List<ShowSeatResponse> seats
) {}
