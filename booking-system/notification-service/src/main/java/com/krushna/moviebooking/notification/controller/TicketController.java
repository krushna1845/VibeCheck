package com.krushna.moviebooking.notification.controller;

import com.krushna.moviebooking.notification.dto.TicketDto;
import com.krushna.moviebooking.notification.dto.TicketRequest;
import com.krushna.moviebooking.notification.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketDto> generateTicket(@Valid @RequestBody TicketRequest request) {
        TicketDto ticketDto = ticketService.generateTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketDto);
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<TicketDto> getByBookingId(@PathVariable UUID bookingId) {
        return ticketService.getTicketByBookingId(bookingId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/reference/{bookingReference}")
    public ResponseEntity<TicketDto> getByBookingReference(@PathVariable String bookingReference) {
        return ticketService.getTicketByBookingReference(bookingReference)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/reference/{bookingReference}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String bookingReference) {
        byte[] pdfBytes = ticketService.getTicketPdfByBookingReference(bookingReference);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Ticket_" + bookingReference + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
