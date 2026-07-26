package com.krushna.moviebooking.theatre.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Inbound DTO for creating or updating a Seat.
 */
@Builder
public record SeatRequest(

        @NotBlank(message = "Seat row is required")
        @Size(max = 10, message = "Seat row must not exceed 10 characters")
        String seatRow,

        @NotNull(message = "Seat number is required")
        Integer seatNumber,

        @NotBlank(message = "Seat category is required")
        @Size(max = 30, message = "Seat category must not exceed 30 characters")
        String seatCategory,

        Boolean isActive
) {}
