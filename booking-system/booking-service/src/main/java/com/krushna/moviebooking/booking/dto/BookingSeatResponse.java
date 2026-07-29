package com.krushna.moviebooking.booking.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Outbound DTO representing individual seat details within a booking.
 *
 * @param id         BookingSeat primary UUID
 * @param showSeatId Reference ID to ShowSeat in Show Service
 * @param seatNumber Human-readable seat identifier (e.g., "A12")
 * @param price      Price charged for this seat
 */
@Builder
public record BookingSeatResponse(
        UUID id,
        UUID showSeatId,
        String seatNumber,
        BigDecimal price
) {}
