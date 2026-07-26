package com.krushna.moviebooking.theatre.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

/**
 * Inbound DTO for creating or updating a Screen.
 */
@Builder
public record ScreenRequest(

        @NotBlank(message = "Screen name is required")
        @Size(max = 50, message = "Screen name must not exceed 50 characters")
        String name,

        @NotBlank(message = "Screen type is required")
        @Size(max = 30, message = "Screen type must not exceed 30 characters")
        String screenType,

        @NotNull(message = "Total seats capacity is required")
        @Min(value = 1, message = "Total seats must be at least 1")
        Integer totalSeats,

        List<SeatRequest> seats
) {}
