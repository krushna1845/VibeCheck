package com.krushna.moviebooking.payment.service;

import com.krushna.moviebooking.payment.dto.PaymentCallback;
import com.krushna.moviebooking.payment.dto.PaymentRequest;
import com.krushna.moviebooking.payment.dto.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Core service interface governing the complete lifecycle of payments.
 *
 * <p>All write operations are transactional. Idempotency keys prevent duplicate charges.
 * Callback handling includes duplicate-callback protection.
 */
public interface PaymentService {

    /**
     * Initiates a new payment or returns a cached response for a duplicate idempotency key.
     *
     * <p>On first call: persists a {@code Payment} record (status=INITIATED), invokes the
     * external gateway via {@link com.krushna.moviebooking.payment.gateway.PaymentClient},
     * caches the response in Redis, and publishes a {@code payment-initiated} event.
     *
     * <p>On duplicate call with same {@code idempotencyKey}: returns cached response without
     * charging again.
     *
     * @param request Validated payment initiation payload
     * @return {@link PaymentResponse} (new or cached)
     */
    PaymentResponse initiatePayment(PaymentRequest request);

    /**
     * Processes an inbound gateway callback, reconciling it with the existing payment record.
     *
     * <p>Duplicate callback protection: if the transaction reference was already processed,
     * returns the existing response idempotently.
     *
     * @param callback Inbound callback from the gateway
     * @return Updated {@link PaymentResponse}
     */
    PaymentResponse processCallback(PaymentCallback callback);

    /**
     * Retrieves payment details by internal payment ID.
     *
     * @param paymentId Internal payment UUID
     * @return {@link PaymentResponse}
     */
    PaymentResponse getPaymentById(UUID paymentId);

    /**
     * Retrieves payment details by booking ID.
     *
     * @param bookingId Booking UUID
     * @return {@link PaymentResponse}
     */
    PaymentResponse getPaymentByBookingId(UUID bookingId);

    /**
     * Returns a pageable list of payments for a user.
     *
     * @param userId   Customer UUID
     * @param pageable Pagination parameters
     * @return Page of {@link PaymentResponse}
     */
    Page<PaymentResponse> getPaymentsByUserId(UUID userId, Pageable pageable);
}
