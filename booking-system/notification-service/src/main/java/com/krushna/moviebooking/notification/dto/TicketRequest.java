package com.krushna.moviebooking.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Builder
public record TicketRequest(
        @NotNull(message = "Booking ID is required")
        UUID bookingId,

        @NotBlank(message = "Booking reference is required")
        String bookingReference,

        UUID userId,

        @NotBlank(message = "Movie title is required")
        String movieTitle,

        @NotBlank(message = "Theatre name is required")
        String theatreName,

        @NotBlank(message = "Screen name is required")
        String screenName,

        @NotBlank(message = "Seat numbers are required")
        String seatNumbers,

        LocalDate showDate,

        LocalTime showTime,

        @NotNull(message = "Amount is required")
        BigDecimal amount,

        String recipientEmail
) {}
