package com.krushna.moviebooking.notification.pdf;

import com.krushna.moviebooking.notification.dto.TicketRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OpenPdfTicketGeneratorTest {

    private OpenPdfTicketGenerator openPdfTicketGenerator;
    private QrCodeGenerator qrCodeGenerator;

    @BeforeEach
    void setUp() {
        qrCodeGenerator = new QrCodeGenerator();
        openPdfTicketGenerator = new OpenPdfTicketGenerator(qrCodeGenerator);
    }

    @Test
    void testGeneratePdfSuccess() {
        TicketRequest request = TicketRequest.builder()
                .bookingId(UUID.randomUUID())
                .bookingReference("VIBE-998877")
                .userId(UUID.randomUUID())
                .movieTitle("Inception: Special Edition")
                .theatreName("IMAX PVR Forum")
                .screenName("Screen 4")
                .seatNumbers("F12, F13")
                .showDate(LocalDate.of(2026, 8, 15))
                .showTime(LocalTime.of(19, 30))
                .amount(new BigDecimal("550.00"))
                .build();

        String qrPayload = "VIBECHECK|TKT-12345|VIBE-998877|" + request.bookingId();

        byte[] pdfBytes = openPdfTicketGenerator.generatePdf(request, qrPayload);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        // Header verification for PDF magic numbers: %PDF
        String headerStr = new String(pdfBytes, 0, 4);
        assertEquals("%PDF", headerStr);
    }
}
