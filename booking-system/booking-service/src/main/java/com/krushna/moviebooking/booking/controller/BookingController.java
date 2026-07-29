package com.krushna.moviebooking.booking.controller;

import com.krushna.moviebooking.booking.dto.*;
import com.krushna.moviebooking.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API controller for booking lifecycle management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /**
     * Creates a new booking in PENDING state and locks requested seats.
     */
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        log.info("REST request to create booking for showId: {}", request.showId());
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing booking using patch semantics.
     */
    @PutMapping("/{id}")
    public ResponseEntity<BookingResponse> updateBooking(
            @PathVariable UUID id,
            @Valid @RequestBody BookingUpdateRequest request) {
        log.info("REST request to update booking with id: {}", id);
        BookingResponse response = bookingService.updateBooking(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Processes payment completion callback to confirm a booking.
     */
    @PostMapping("/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(@Valid @RequestBody PaymentCallbackRequest callback) {
        log.info("REST request to confirm booking for reference: {}", callback.bookingReference());
        BookingResponse response = bookingService.confirmBooking(callback.bookingReference(), callback.paymentId());
        return ResponseEntity.ok(response);
    }

    /**
     * Cancels a booking reservation.
     */
    @PostMapping("/{reference}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable String reference,
            @RequestParam(required = false, defaultValue = "Customer cancelled") String reason) {
        log.info("REST request to cancel booking reference: {}", reference);
        BookingResponse response = bookingService.cancelBooking(reference, reason);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft-deletes a booking reservation.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable UUID id) {
        log.info("REST request to soft-delete booking id: {}", id);
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Gets booking details by reference string.
     */
    @GetMapping("/{reference}")
    public ResponseEntity<BookingResponse> getBookingByReference(@PathVariable String reference) {
        log.debug("REST request to fetch booking by reference: {}", reference);
        BookingResponse response = bookingService.getBookingByReference(reference);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets booking details by primary UUID.
     */
    @GetMapping("/id/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable UUID id) {
        log.debug("REST request to fetch booking by ID: {}", id);
        BookingResponse response = bookingService.getBookingById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets paged summary of all active bookings.
     */
    @GetMapping
    public ResponseEntity<Page<BookingSummary>> getAllBookings(Pageable pageable) {
        log.debug("REST request to fetch all bookings");
        Page<BookingSummary> response = bookingService.getAllBookings(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets paged bookings for a user.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<BookingSummary>> getUserBookings(
            @PathVariable UUID userId,
            Pageable pageable) {
        log.debug("REST request to fetch bookings for userId: {}", userId);
        Page<BookingSummary> response = bookingService.getUserBookings(userId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets confirmed bookings for a show.
     */
    @GetMapping("/show/{showId}")
    public ResponseEntity<List<BookingSummary>> getShowBookings(@PathVariable UUID showId) {
        log.debug("REST request to fetch bookings for showId: {}", showId);
        List<BookingSummary> response = bookingService.getShowBookings(showId);
        return ResponseEntity.ok(response);
    }
}
