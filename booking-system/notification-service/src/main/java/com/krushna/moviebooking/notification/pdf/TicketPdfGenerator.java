package com.krushna.moviebooking.notification.pdf;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Generates a styled PDF booking ticket with embedded QR code using iText7.
 */
@Component
public class TicketPdfGenerator {

    private static final Logger log = LoggerFactory.getLogger(TicketPdfGenerator.class);
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a").withZone(ZoneId.systemDefault());

    private final QrCodeGenerator qrCodeGenerator;

    public TicketPdfGenerator(QrCodeGenerator qrCodeGenerator) {
        this.qrCodeGenerator = qrCodeGenerator;
    }

    /**
     * Generates a booking ticket PDF.
     *
     * @param data map with: bookingReference, userName, showTitle, seatNumbers, venue, amount, paymentId
     * @return PDF bytes
     */
    public byte[] generate(Map<String, Object> data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            document.setMargins(30, 40, 30, 40);

            DeviceRgb headerColor = new DeviceRgb(55, 71, 179);   // Indigo
            DeviceRgb accentColor = new DeviceRgb(251, 192, 45);  // Gold

            // ── Header ──────────────────────────────────────
            Paragraph header = new Paragraph("🎬 VibeCheck")
                    .setFontSize(22)
                    .setBold()
                    .setFontColor(ColorConstants.WHITE)
                    .setBackgroundColor(headerColor)
                    .setPadding(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setWidth(UnitValue.createPercentValue(100));
            document.add(header);

            Paragraph title = new Paragraph("BOOKING CONFIRMATION")
                    .setFontSize(13)
                    .setBold()
                    .setFontColor(accentColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(4);
            document.add(title);

            document.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f))
                    .setMarginTop(6).setMarginBottom(10));

            // ── Ticket Details Table ──────────────────────
            float[] colWidths = {2, 3};
            Table table = new Table(UnitValue.createPercentArray(colWidths))
                    .setWidth(UnitValue.createPercentValue(100));

            addRow(table, "Booking Reference", str(data, "bookingReference"));
            addRow(table, "Passenger Name", str(data, "userName"));
            addRow(table, "Movie / Show", str(data, "showTitle"));
            addRow(table, "Seat(s)", str(data, "seatNumbers"));
            addRow(table, "Venue", str(data, "venue"));
            addRow(table, "Show Time", str(data, "showTime"));
            addRow(table, "Amount Paid", "₹ " + str(data, "amount"));
            addRow(table, "Transaction ID", str(data, "paymentId"));
            addRow(table, "Issued At", FORMATTER.format(Instant.now()));

            document.add(table);

            // ── QR Code ────────────────────────────────────
            String qrContent = "VIBECHECK|" + str(data, "bookingReference") + "|" + str(data, "paymentId");
            byte[] qrBytes = qrCodeGenerator.generatePng(qrContent);

            Image qrImage = new Image(ImageDataFactory.create(qrBytes))
                    .setWidth(100)
                    .setHeight(100)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER)
                    .setMarginTop(15);
            document.add(qrImage);

            Paragraph qrLabel = new Paragraph("Scan QR at venue entrance")
                    .setFontSize(8)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(2);
            document.add(qrLabel);

            // ── Footer ──────────────────────────────────────
            document.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f))
                    .setMarginTop(12));
            Paragraph footer = new Paragraph("Thank you for booking with VibeCheck! Enjoy your show.")
                    .setFontSize(9)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(6);
            document.add(footer);

            document.close();
            log.debug("PDF ticket generated for booking={}", data.get("bookingReference"));
        } catch (Exception e) {
            log.error("Failed to generate PDF ticket: {}", e.getMessage(), e);
            throw new RuntimeException("PDF generation failed", e);
        }
        return out.toByteArray();
    }

    private void addRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold().setFontSize(10))
                .setBorderRight(new com.itextpdf.layout.borders.SolidBorder(0.5f)));
        table.addCell(new Cell().add(new Paragraph(value != null ? value : "N/A").setFontSize(10)));
    }

    private String str(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "N/A";
    }
}
