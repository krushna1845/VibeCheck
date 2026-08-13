package com.krushna.moviebooking.payment.controller;

import com.krushna.moviebooking.payment.dto.PaymentCallback;
import com.krushna.moviebooking.payment.dto.PaymentRequest;
import com.krushna.moviebooking.payment.dto.PaymentResponse;
import com.krushna.moviebooking.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST API controller for payment lifecycle management.
 *
 * <p>All endpoints follow REST conventions. The {@code /callback} endpoint is intentionally
 * unauthenticated (permit-all in security config) and protected instead by HMAC-SHA256
 * signature verification in the service layer.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "Endpoints for initiating, fetching, and refunding payments")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Initiates a new payment or returns a cached response for duplicate idempotency keys.
     *
     * <p>The caller must supply a unique {@code idempotencyKey} in the request body.
     * Retrying with the same key is safe and returns the original response.
     *
     * @param request Validated payment initiation payload
     * @return 201 Created on first initiation; 200 OK on idempotent repeat
     */
    @Operation(summary = "Initiate Payment", description = "Initiates a new payment session or returns an idempotent cached response if idempotencyKey has been processed previously.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Payment initiated successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payment request payload or validation failure"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "409", description = "Payment state conflict or duplicate initiation under conflicting parameters"),
            @ApiResponse(responseCode = "500", description = "Internal payment gateway or service failure")
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> initiatePayment(@Valid @RequestBody PaymentRequest request) {
        log.info("[PaymentController] POST /api/v1/payments | idempotencyKey={} bookingRef={}",
                request.idempotencyKey(), request.bookingReference());
        PaymentResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves payment details by internal payment UUID.
     *
     * @param paymentId Internal payment UUID
     * @return 200 OK with payment details
     */
    @Operation(summary = "Get Payment by ID", description = "Fetches complete payment details using internal payment UUID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Payment not found for given UUID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access")
    })
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @Parameter(description = "Internal payment UUID", required = true) @PathVariable UUID paymentId) {
        log.debug("[PaymentController] GET /api/v1/payments/{}", paymentId);
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }

    /**
     * Retrieves payment details by associated booking UUID.
     *
     * @param bookingId Booking UUID
     * @return 200 OK with payment details
     */
    @Operation(summary = "Get Payment by Booking ID", description = "Fetches payment information associated with a given booking UUID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "404", description = "No payment record found for given booking UUID")
    })
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> getPaymentByBookingId(
            @Parameter(description = "Associated Booking UUID", required = true) @PathVariable UUID bookingId) {
        log.debug("[PaymentController] GET /api/v1/payments/booking/{}", bookingId);
        return ResponseEntity.ok(paymentService.getPaymentByBookingId(bookingId));
    }

    /**
     * Returns a pageable list of payments for a specific user.
     *
     * @param userId   Customer UUID
     * @param pageable Pagination parameters
     * @return 200 OK with page of payment summaries
     */
    @Operation(summary = "Get Payments by User ID", description = "Retrieves a paginated list of all payments associated with a customer user UUID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated payment history retrieved successfully")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<PaymentResponse>> getPaymentsByUser(
            @Parameter(description = "Customer User UUID", required = true) @PathVariable UUID userId,
            Pageable pageable) {
        log.debug("[PaymentController] GET /api/v1/payments/user/{}", userId);
        return ResponseEntity.ok(paymentService.getPaymentsByUserId(userId, pageable));
    }

    /**
     * Processes a full or partial refund for a payment.
     *
     * @param paymentId Internal payment UUID
     * @param request   Validated refund payload
     * @return 200 OK with refund confirmation
     */
    @Operation(summary = "Process Payment Refund", description = "Processes full or partial refund for a completed payment transaction.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Refund processed successfully",
                    content = @Content(schema = @Schema(implementation = com.krushna.moviebooking.payment.dto.RefundResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid refund amount or parameters"),
            @ApiResponse(responseCode = "404", description = "Payment ID not found"),
            @ApiResponse(responseCode = "422", description = "Unprocessable refund due to payment state")
    })
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<com.krushna.moviebooking.payment.dto.RefundResponse> processRefund(
            @Parameter(description = "Internal payment UUID", required = true) @PathVariable UUID paymentId,
            @Valid @RequestBody com.krushna.moviebooking.payment.dto.RefundRequest request) {
        log.info("[PaymentController] POST /api/v1/payments/{}/refund | amount={}", paymentId, request.amount());
        com.krushna.moviebooking.payment.dto.RefundResponse response = paymentService.processRefund(request);
        return ResponseEntity.ok(response);
    }
}

