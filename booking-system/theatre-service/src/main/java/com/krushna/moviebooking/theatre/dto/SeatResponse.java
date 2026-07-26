package com.krushna.moviebooking.theatre.dto;

import lombok.Builder;

import java.util.UUID;

/**
 * Outbound DTO returned from Seat service operations.
 */
@Builder
public record SeatResponse(
        UUID id,
        UUID screenId,
        String seatRow,
        Integer seatNumber,
        String seatCategory,
        Boolean isActive
) {}
