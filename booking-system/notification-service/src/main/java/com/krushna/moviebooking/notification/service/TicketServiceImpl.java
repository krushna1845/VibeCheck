package com.krushna.moviebooking.notification.service;

import com.krushna.moviebooking.notification.dto.TicketDto;
import com.krushna.moviebooking.notification.dto.TicketRequest;
import com.krushna.moviebooking.notification.entity.Ticket;
import com.krushna.moviebooking.notification.entity.TicketStatus;
import com.krushna.moviebooking.notification.pdf.OpenPdfTicketGenerator;
import com.krushna.moviebooking.notification.repository.TicketRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class TicketServiceImpl implements TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketServiceImpl.class);

    private final TicketRepository ticketRepository;
    private final OpenPdfTicketGenerator openPdfTicketGenerator;
    private final Counter ticketGeneratedCounter;

    public TicketServiceImpl(
            TicketRepository ticketRepository,
            OpenPdfTicketGenerator openPdfTicketGenerator,
            MeterRegistry meterRegistry) {
        this.ticketRepository = ticketRepository;
        this.openPdfTicketGenerator = openPdfTicketGenerator;
        this.ticketGeneratedCounter = meterRegistry.counter("tickets.generated");
    }

    @Override
    @Transactional
    public TicketDto generateTicket(TicketRequest request) {
        log.info("Generating production ticket for bookingRef={}, bookingId={}",
                request.bookingReference(), request.bookingId());

        String ticketNumber = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String qrPayload = String.format("VIBECHECK|%s|%s|%s",
                ticketNumber, request.bookingReference(), request.bookingId());

        // 1. Generate PDF with embedded QR code using OpenPDF
        byte[] pdfBytes = openPdfTicketGenerator.generatePdf(request, qrPayload);

        // 2. Build and save Ticket entity metadata
        Ticket ticket = Ticket.builder()
                .ticketNumber(ticketNumber)
                .bookingId(request.bookingId())
                .bookingReference(request.bookingReference())
                .userId(request.userId())
                .movieTitle(request.movieTitle())
                .theatreName(request.theatreName())
                .screenName(request.screenName())
                .seatNumbers(request.seatNumbers())
                .showDate(request.showDate())
                .showTime(request.showTime())
                .amount(request.amount())
                .qrCodeData(qrPayload)
                .status(TicketStatus.GENERATED)
                .build();

        ticket = ticketRepository.save(ticket);
        ticketGeneratedCounter.increment();

        log.info("Successfully stored ticket metadata id={} number={}", ticket.getId(), ticket.getTicketNumber());

        return mapToDto(ticket, pdfBytes);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TicketDto> getTicketByBookingId(UUID bookingId) {
        return ticketRepository.findByBookingId(bookingId)
                .map(ticket -> mapToDto(ticket, null));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TicketDto> getTicketByBookingReference(String bookingReference) {
        return ticketRepository.findByBookingReference(bookingReference)
                .map(ticket -> mapToDto(ticket, null));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getTicketPdfByBookingReference(String bookingReference) {
        Ticket ticket = ticketRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found for booking reference: " + bookingReference));

        TicketRequest request = TicketRequest.builder()
                .bookingId(ticket.getBookingId())
                .bookingReference(ticket.getBookingReference())
                .userId(ticket.getUserId())
                .movieTitle(ticket.getMovieTitle())
                .theatreName(ticket.getTheatreName())
                .screenName(ticket.getScreenName())
                .seatNumbers(ticket.getSeatNumbers())
                .showDate(ticket.getShowDate())
                .showTime(ticket.getShowTime())
                .amount(ticket.getAmount())
                .build();

        return openPdfTicketGenerator.generatePdf(request, ticket.getQrCodeData());
    }

    private TicketDto mapToDto(Ticket ticket, byte[] pdfBytes) {
        return TicketDto.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .bookingId(ticket.getBookingId())
                .bookingReference(ticket.getBookingReference())
                .userId(ticket.getUserId())
                .movieTitle(ticket.getMovieTitle())
                .theatreName(ticket.getTheatreName())
                .screenName(ticket.getScreenName())
                .seatNumbers(ticket.getSeatNumbers())
                .showDate(ticket.getShowDate())
                .showTime(ticket.getShowTime())
                .amount(ticket.getAmount())
                .qrCodeData(ticket.getQrCodeData())
                .status(ticket.getStatus())
                .pdfBytes(pdfBytes)
                .createdAt(ticket.getCreatedAt())
                .build();
    }
}
