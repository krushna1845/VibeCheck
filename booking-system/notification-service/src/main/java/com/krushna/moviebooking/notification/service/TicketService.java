package com.krushna.moviebooking.notification.service;

import com.krushna.moviebooking.notification.dto.TicketDto;
import com.krushna.moviebooking.notification.dto.TicketRequest;

import java.util.Optional;
import java.util.UUID;

public interface TicketService {
    /**
     * Generates PDF ticket with embedded QR code, stores metadata in DB, and returns TicketDto.
     */
    TicketDto generateTicket(TicketRequest request);

    /**
     * Retrieves ticket metadata by booking ID.
     */
    Optional<TicketDto> getTicketByBookingId(UUID bookingId);

    /**
     * Retrieves ticket metadata by booking reference.
     */
    Optional<TicketDto> getTicketByBookingReference(String bookingReference);

    /**
     * Retrieves raw PDF bytes for a booking reference.
     */
    byte[] getTicketPdfByBookingReference(String bookingReference);
}
