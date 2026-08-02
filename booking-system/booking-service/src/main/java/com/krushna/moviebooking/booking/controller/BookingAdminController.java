package com.krushna.moviebooking.booking.controller;

import com.krushna.moviebooking.booking.dto.BookingResponse;
import com.krushna.moviebooking.booking.dto.BookingSearchCriteria;
import com.krushna.moviebooking.booking.dto.BookingSummary;
import com.krushna.moviebooking.booking.dto.BookingUpdateRequest;
import com.krushna.moviebooking.common.dto.ApiResponse;
import com.krushna.moviebooking.common.dto.PagedResponse;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Administrative REST API controller for global booking search, inspection, and status management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/bookings")
@RequiredArgsConstructor
@Tag(name = "Admin Booking Management", description = "Administrative operations for searching, filtering, and force-updating bookings")
public class BookingAdminController {

    private final BookingService bookingService;

    @Operation(summary = "Search bookings with filtering, sorting, and pagination",
            description = "Allows administrators to query bookings by status, userId, showId, and date range.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search query processed successfully")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<BookingSummary>>> searchBookings(
            @Valid @ModelAttribute BookingSearchCriteria criteria,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Admin search request: criteria={}, pageable={}", criteria, pageable);
        Page<BookingSummary> page = bookingService.adminSearch(criteria, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(page), "Admin search completed successfully"));
    }

    @Operation(summary = "Get detailed booking by ID (Admin)",
            description = "Retrieves full details for any booking by primary UUID.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Booking details retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @Parameter(description = "Primary UUID of the booking", required = true)
            @PathVariable UUID id) {
        log.info("Admin request to fetch booking details for ID: {}", id);
        BookingResponse response = bookingService.getBookingById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Force update booking status (Admin)",
            description = "Allows administrators to forcefully update a booking's status or metadata.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Booking status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status transition or request body",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<BookingResponse>> forceUpdateStatus(
            @Parameter(description = "Primary UUID of the booking", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody BookingUpdateRequest request) {
        log.info("Admin request to force update status for booking ID {}: status={}", id, request.status());
        BookingResponse response = bookingService.updateBooking(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Booking status updated by administrator"));
    }
}
