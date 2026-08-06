package com.krushna.moviebooking.payment.controller;

import com.krushna.moviebooking.payment.dto.PaymentCallback;
import com.krushna.moviebooking.payment.dto.PaymentRequest;
import com.krushna.moviebooking.payment.dto.PaymentResponse;
import com.krushna.moviebooking.payment.service.PaymentService;
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
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable UUID paymentId) {
        log.debug("[PaymentController] GET /api/v1/payments/{}", paymentId);
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }

    /**
     * Retrieves payment details by associated booking UUID.
     *
     * @param bookingId Booking UUID
     * @return 200 OK with payment details
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> getPaymentByBookingId(@PathVariable UUID bookingId) {
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
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<PaymentResponse>> getPaymentsByUser(
            @PathVariable UUID userId,
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
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<com.krushna.moviebooking.payment.dto.RefundResponse> processRefund(
            @PathVariable UUID paymentId,
            @Valid @RequestBody com.krushna.moviebooking.payment.dto.RefundRequest request) {
        log.info("[PaymentController] POST /api/v1/payments/{}/refund | amount={}", paymentId, request.amount());
        com.krushna.moviebooking.payment.dto.RefundResponse response = paymentService.processRefund(request);
        return ResponseEntity.ok(response);
    }
}
