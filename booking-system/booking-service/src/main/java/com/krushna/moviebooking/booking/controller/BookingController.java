package com.krushna.moviebooking.booking.controller;

import com.krushna.moviebooking.booking.dto.*;
import com.krushna.moviebooking.booking.model.ApiResponse;
import com.krushna.moviebooking.booking.model.PagedResponse;
import com.krushna.moviebooking.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API controller for customer booking lifecycle management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking Management", description = "Endpoints for managing customer movie ticket bookings, confirmations, and cancellations")
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "Create a new booking", description = "Reserves selected seats and creates a booking in PENDING status with temporary Redis locks.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Booking created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Seat lock conflict or seat unavailable",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingRequest request) {
        log.info("REST request to create booking for showId: {}", request.showId());
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Booking created successfully"));
    }

    @Operation(summary = "Update an existing booking", description = "Updates booking details using patch semantics.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Booking updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBooking(
            @Parameter(description = "Primary UUID of the booking", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody BookingUpdateRequest request) {
        log.info("REST request to update booking with id: {}", id);
        BookingResponse response = bookingService.updateBooking(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Booking updated successfully"));
    }

    @Operation(summary = "Confirm booking payment", description = "Processes payment confirmation callback and transitions booking to CONFIRMED state.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Booking confirmed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "Booking reservation expired",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @Valid @RequestBody PaymentCallbackRequest callback) {
        log.info("REST request to confirm booking for reference: {}", callback.bookingReference());
        BookingResponse response = bookingService.confirmBooking(callback.bookingReference(), callback.paymentId());
        return ResponseEntity.ok(ApiResponse.success(response, "Booking confirmed successfully"));
    }

    @Operation(summary = "Cancel a booking", description = "Cancels a booking reservation and releases locked seats.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Booking cancelled successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{reference}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @Parameter(description = "12-character booking reference", required = true)
            @PathVariable String reference,
            @Parameter(description = "Reason for cancellation")
            @RequestParam(required = false, defaultValue = "Customer cancelled") String reason) {
        log.info("REST request to cancel booking reference: {}", reference);
        BookingResponse response = bookingService.cancelBooking(reference, reason);
        return ResponseEntity.ok(ApiResponse.success(response, "Booking cancelled successfully"));
    }

    @Operation(summary = "Soft-delete a booking", description = "Marks a booking as soft-deleted.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Booking deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(
            @Parameter(description = "Primary UUID of the booking", required = true)
            @PathVariable UUID id) {
        log.info("REST request to soft-delete booking id: {}", id);
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get booking by reference", description = "Retrieves detailed booking information using 12-character reference code.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Booking found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{reference}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByReference(
            @Parameter(description = "12-character booking reference", required = true)
            @PathVariable String reference) {
        log.debug("REST request to fetch booking by reference: {}", reference);
        BookingResponse response = bookingService.getBookingByReference(reference);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get booking by ID", description = "Retrieves detailed booking information using primary UUID.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Booking found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/id/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @Parameter(description = "Primary UUID of the booking", required = true)
            @PathVariable UUID id) {
        log.debug("REST request to fetch booking by ID: {}", id);
        BookingResponse response = bookingService.getBookingById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get all active bookings", description = "Retrieves a paginated list of all active booking summaries.")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<BookingSummary>>> getAllBookings(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.debug("REST request to fetch all bookings");
        Page<BookingSummary> page = bookingService.getAllBookings(pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(page)));
    }

    @Operation(summary = "Get user booking history", description = "Retrieves a paginated list of booking summaries for a specific user.")
    @GetMapping("/user/{userId}/history")
    public ResponseEntity<ApiResponse<PagedResponse<BookingSummary>>> getUserBookingHistory(
            @Parameter(description = "User UUID", required = true)
            @PathVariable UUID userId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.debug("REST request to fetch booking history for userId: {}", userId);
        Page<BookingSummary> page = bookingService.getUserBookings(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(page)));
    }

    @Operation(summary = "Get user bookings", description = "Alias for user booking history.")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PagedResponse<BookingSummary>>> getUserBookings(
            @Parameter(description = "User UUID", required = true)
            @PathVariable UUID userId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return getUserBookingHistory(userId, pageable);
    }

    @Operation(summary = "Get show bookings", description = "Retrieves all confirmed booking summaries for a given show.")
    @GetMapping("/show/{showId}")
    public ResponseEntity<ApiResponse<List<BookingSummary>>> getShowBookings(
            @Parameter(description = "Show UUID", required = true)
            @PathVariable UUID showId) {
        log.debug("REST request to fetch bookings for showId: {}", showId);
        List<BookingSummary> response = bookingService.getShowBookings(showId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
