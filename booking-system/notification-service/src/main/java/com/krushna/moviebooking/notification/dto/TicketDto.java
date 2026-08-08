package com.krushna.moviebooking.notification.dto;

import com.krushna.moviebooking.notification.entity.TicketStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Builder
public record TicketDto(
        UUID id,
        String ticketNumber,
        UUID bookingId,
        String bookingReference,
        UUID userId,
        String movieTitle,
        String theatreName,
        String screenName,
        String seatNumbers,
        LocalDate showDate,
        LocalTime showTime,
        BigDecimal amount,
        String qrCodeData,
        TicketStatus status,
        byte[] pdfBytes,
        Instant createdAt
) {}
