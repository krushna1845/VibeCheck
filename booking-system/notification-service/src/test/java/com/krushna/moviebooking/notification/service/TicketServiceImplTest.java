package com.krushna.moviebooking.notification.service;

import com.krushna.moviebooking.notification.dto.TicketDto;
import com.krushna.moviebooking.notification.dto.TicketRequest;
import com.krushna.moviebooking.notification.entity.Ticket;
import com.krushna.moviebooking.notification.entity.TicketStatus;
import com.krushna.moviebooking.notification.pdf.OpenPdfTicketGenerator;
import com.krushna.moviebooking.notification.repository.TicketRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private OpenPdfTicketGenerator openPdfTicketGenerator;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);
        ticketService = new TicketServiceImpl(ticketRepository, openPdfTicketGenerator, meterRegistry);
    }

    @Test
    void testGenerateTicketSuccess() {
        UUID bookingId = UUID.randomUUID();
        TicketRequest request = TicketRequest.builder()
                .bookingId(bookingId)
                .bookingReference("REF-12345")
                .userId(UUID.randomUUID())
                .movieTitle("Interstellar")
                .theatreName("PVR Cinemas")
                .screenName("Screen 1")
                .seatNumbers("A1, A2")
                .showDate(LocalDate.now())
                .showTime(LocalTime.now())
                .amount(new BigDecimal("400.00"))
                .build();

        byte[] fakePdfBytes = "%PDF-1.4 Mock Ticket PDF Bytes".getBytes();
        when(openPdfTicketGenerator.generatePdf(any(), anyString())).thenReturn(fakePdfBytes);

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TicketDto result = ticketService.generateTicket(request);

        assertNotNull(result);
        assertEquals("REF-12345", result.bookingReference());
        assertEquals("Interstellar", result.movieTitle());
        assertEquals("PVR Cinemas", result.theatreName());
        assertEquals("Screen 1", result.screenName());
        assertEquals("A1, A2", result.seatNumbers());
        assertEquals(TicketStatus.GENERATED, result.status());
        assertArrayEquals(fakePdfBytes, result.pdfBytes());

        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(counter, times(1)).increment();
    }

    @Test
    void testGetTicketByBookingId() {
        UUID bookingId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .ticketNumber("TKT-1001")
                .bookingId(bookingId)
                .bookingReference("REF-1001")
                .movieTitle("Batman")
                .theatreName("Cinepolis")
                .screenName("Screen 2")
                .seatNumbers("B5")
                .amount(new BigDecimal("300.00"))
                .qrCodeData("QR-DATA")
                .status(TicketStatus.GENERATED)
                .build();

        when(ticketRepository.findByBookingId(bookingId)).thenReturn(Optional.of(ticket));

        Optional<TicketDto> result = ticketService.getTicketByBookingId(bookingId);

        assertTrue(result.isPresent());
        assertEquals("TKT-1001", result.get().ticketNumber());
        assertEquals("REF-1001", result.get().bookingReference());
    }

    @Test
    void testGetTicketByBookingReference() {
        String ref = "REF-2002";
        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .ticketNumber("TKT-2002")
                .bookingId(UUID.randomUUID())
                .bookingReference(ref)
                .movieTitle("Avatar")
                .theatreName("INOX")
                .screenName("IMAX")
                .seatNumbers("C1, C2")
                .amount(new BigDecimal("700.00"))
                .qrCodeData("QR-DATA-2")
                .status(TicketStatus.GENERATED)
                .build();

        when(ticketRepository.findByBookingReference(ref)).thenReturn(Optional.of(ticket));

        Optional<TicketDto> result = ticketService.getTicketByBookingReference(ref);

        assertTrue(result.isPresent());
        assertEquals("TKT-2002", result.get().ticketNumber());
        assertEquals(ref, result.get().bookingReference());
    }

    @Test
    void testGetTicketPdfByBookingReferenceSuccess() {
        String ref = "REF-3003";
        UUID bookingId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .ticketNumber("TKT-3003")
                .bookingId(bookingId)
                .bookingReference(ref)
                .movieTitle("Tenet")
                .theatreName("PVR Luxe")
                .screenName("Screen 1")
                .seatNumbers("D1")
                .amount(new BigDecimal("450.00"))
                .qrCodeData("QR-DATA-3")
                .status(TicketStatus.GENERATED)
                .build();

        byte[] fakePdfBytes = "%PDF-1.4 PDF Content".getBytes();
        when(ticketRepository.findByBookingReference(ref)).thenReturn(Optional.of(ticket));
        when(openPdfTicketGenerator.generatePdf(any(), eq("QR-DATA-3"))).thenReturn(fakePdfBytes);

        byte[] pdfBytes = ticketService.getTicketPdfByBookingReference(ref);

        assertNotNull(pdfBytes);
        assertArrayEquals(fakePdfBytes, pdfBytes);
    }
}
