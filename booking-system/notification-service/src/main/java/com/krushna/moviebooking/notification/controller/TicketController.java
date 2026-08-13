package com.krushna.moviebooking.notification.controller;

import com.krushna.moviebooking.notification.dto.TicketDto;
import com.krushna.moviebooking.notification.dto.TicketRequest;
import com.krushna.moviebooking.notification.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@Tag(name = "Ticket Management", description = "Ticket generation, metadata retrieval, and PDF download endpoints")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "Generate Ticket", description = "Generates ticket metadata, QR code, and PDF for a confirmed booking.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Ticket generated successfully",
                    content = @Content(schema = @Schema(implementation = TicketDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    @PostMapping
    public ResponseEntity<TicketDto> generateTicket(@Valid @RequestBody TicketRequest request) {
        TicketDto ticketDto = ticketService.generateTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketDto);
    }

    @Operation(summary = "Get Ticket by Booking ID", description = "Retrieves ticket metadata by associated booking UUID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ticket retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TicketDto.class))),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<TicketDto> getByBookingId(
            @Parameter(description = "Booking UUID", required = true) @PathVariable UUID bookingId) {
        return ticketService.getTicketByBookingId(bookingId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get Ticket by Booking Reference", description = "Retrieves ticket metadata by booking reference code.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ticket retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TicketDto.class))),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @GetMapping("/reference/{bookingReference}")
    public ResponseEntity<TicketDto> getByBookingReference(
            @Parameter(description = "Booking Reference String", required = true) @PathVariable String bookingReference) {
        return ticketService.getTicketByBookingReference(bookingReference)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Download Ticket PDF", description = "Generates and streams the ticket PDF file for download.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF ticket generated and returned as binary attachment",
                    content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)),
            @ApiResponse(responseCode = "404", description = "Ticket not found for given reference")
    })
    @GetMapping("/reference/{bookingReference}/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @Parameter(description = "Booking Reference String", required = true) @PathVariable String bookingReference) {
        byte[] pdfBytes = ticketService.getTicketPdfByBookingReference(bookingReference);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Ticket_" + bookingReference + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}

