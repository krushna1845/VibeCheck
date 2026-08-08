package com.krushna.moviebooking.notification.pdf;

import com.krushna.moviebooking.notification.dto.TicketRequest;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Production-ready PDF ticket generator using OpenPDF (com.lowagie.text).
 */
@Component
public class OpenPdfTicketGenerator {

    private static final Logger log = LoggerFactory.getLogger(OpenPdfTicketGenerator.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter ISSUED_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a").withZone(ZoneId.systemDefault());

    private final QrCodeGenerator qrCodeGenerator;

    public OpenPdfTicketGenerator(QrCodeGenerator qrCodeGenerator) {
        this.qrCodeGenerator = qrCodeGenerator;
    }

    /**
     * Generates PDF bytes for a ticket request.
     *
     * @param request ticket details
     * @param qrPayload QR code content string
     * @return generated PDF bytes
     */
    public byte[] generatePdf(TicketRequest request, String qrPayload) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Colors & Fonts
            Color primaryColor = new Color(41, 128, 185);   // Vibrant Blue
            Color darkTextColor = new Color(44, 62, 80);    // Dark Gray/Blue
            Color grayColor = new Color(127, 140, 141);     // Muted Gray

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, primaryColor);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, darkTextColor);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, darkTextColor);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, darkTextColor);
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 9, grayColor);

            // Header Banner
            Paragraph header = new Paragraph("VibeCheck Movie Ticket", titleFont);
            header.setAlignment(Element.ALIGN_CENTER);
            header.setSpacingAfter(4);
            document.add(header);

            Paragraph subtitle = new Paragraph("BOOKING CONFIRMATION", subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(10);
            document.add(subtitle);

            // Separator Line
            LineSeparator line = new LineSeparator(1.5f, 100, primaryColor, Element.ALIGN_CENTER, -2);
            document.add(line);
            document.add(Chunk.NEWLINE);

            // Details Table
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 3.5f});
            table.setSpacingBefore(10f);
            table.setSpacingAfter(15f);

            addTableRow(table, "Booking Reference:", request.bookingReference(), labelFont, valueFont);
            addTableRow(table, "Booking ID:", request.bookingId() != null ? request.bookingId().toString() : "N/A", labelFont, valueFont);
            addTableRow(table, "Movie:", request.movieTitle(), labelFont, valueFont);
            addTableRow(table, "Theatre:", request.theatreName(), labelFont, valueFont);
            addTableRow(table, "Screen:", request.screenName(), labelFont, valueFont);
            addTableRow(table, "Seat(s):", request.seatNumbers(), labelFont, valueFont);
            addTableRow(table, "Date:", request.showDate() != null ? request.showDate().format(DATE_FORMATTER) : "N/A", labelFont, valueFont);
            addTableRow(table, "Time:", request.showTime() != null ? request.showTime().format(TIME_FORMATTER) : "N/A", labelFont, valueFont);
            addTableRow(table, "Total Amount:", "₹ " + (request.amount() != null ? request.amount().toString() : "0.00"), labelFont, valueFont);
            addTableRow(table, "Issued At:", ISSUED_FORMATTER.format(Instant.now()), labelFont, valueFont);

            document.add(table);

            // QR Code Generation & Embedding
            byte[] qrBytes = qrCodeGenerator.generatePng(qrPayload);
            Image qrImage = Image.getInstance(qrBytes);
            qrImage.setAlignment(Element.ALIGN_CENTER);
            qrImage.scaleToFit(120, 120);
            qrImage.setSpacingBefore(10);
            qrImage.setSpacingAfter(5);
            document.add(qrImage);

            Paragraph qrText = new Paragraph("Scan QR code at theatre entrance for verification", footerFont);
            qrText.setAlignment(Element.ALIGN_CENTER);
            qrText.setSpacingAfter(15);
            document.add(qrText);

            // Footer
            document.add(new LineSeparator(0.8f, 100, grayColor, Element.ALIGN_CENTER, -2));
            Paragraph footer = new Paragraph("Thank you for choosing VibeCheck! Enjoy your movie.", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(8);
            document.add(footer);

            document.close();
            log.info("Successfully generated OpenPDF ticket for bookingReference={}", request.bookingReference());
        } catch (Exception e) {
            log.error("Failed to generate OpenPDF ticket: {}", e.getMessage(), e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    private void addTableRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(label, labelFont));
        cellLabel.setBorder(Rectangle.NO_BORDER);
        cellLabel.setPadding(6);
        cellLabel.setBackgroundColor(new Color(245, 247, 250));

        PdfPCell cellValue = new PdfPCell(new Phrase(value != null ? value : "N/A", valueFont));
        cellValue.setBorder(Rectangle.NO_BORDER);
        cellValue.setPadding(6);
        cellValue.setBackgroundColor(new Color(245, 247, 250));

        table.addCell(cellLabel);
        table.addCell(cellValue);
    }
}
