package com.krushna.moviebooking.payment.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Outbound DTO returned by the payment service after processing a payment initiation.
 *
 * @param paymentId            Internally assigned payment UUID
 * @param bookingId            Associated booking UUID
 * @param bookingReference     12-character human-readable booking reference
 * @param idempotencyKey       Echo of the caller-supplied idempotency key
 * @param transactionReference External gateway transaction reference
 * @param status               Payment status (INITIATED / SUCCESS / FAILED / REFUNDED)
 * @param amount               Charged amount
 * @param currency             Currency of the charge
 * @param paymentMethod        Payment method used
 * @param redirectUrl          Gateway checkout URL (may be null for server-side flows)
 * @param failureReason        Non-null only when status is FAILED
 * @param createdAt            Timestamp when the payment record was created
 */
@Builder
public record PaymentResponse(
        UUID paymentId,
        UUID bookingId,
        String bookingReference,
        String idempotencyKey,
        String transactionReference,
        String status,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String redirectUrl,
        String failureReason,
        Instant createdAt
) {}
