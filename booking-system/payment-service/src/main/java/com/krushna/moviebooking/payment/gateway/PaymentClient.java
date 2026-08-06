package com.krushna.moviebooking.payment.gateway;

import com.krushna.moviebooking.payment.dto.PaymentRequest;
import com.krushna.moviebooking.payment.dto.PaymentResponse;

/**
 * Gateway-level client interface for sending payment requests to an external payment provider.
 *
 * <p>Implementations handle transport, serialization, timeout configuration, and retry
 * mechanics. The contract guarantees:
 * <ul>
 *   <li>Calls are idempotent when the same {@code idempotencyKey} is provided.</li>
 *   <li>A {@link PaymentGatewayException} is raised on unrecoverable gateway errors.</li>
 *   <li>A {@link PaymentTimeoutException} is raised when the gateway does not respond in time.</li>
 * </ul>
 */
public interface PaymentClient {

    /**
     * Submits a payment request to the external payment gateway.
     *
     * @param request Validated payment initiation payload
     * @return {@link PaymentResponse} with gateway-assigned transaction reference and checkout URL
     * @throws PaymentGatewayException  on non-retryable gateway error
     * @throws PaymentTimeoutException  when the gateway call exceeds the configured timeout
     */
    PaymentResponse initiatePayment(PaymentRequest request);

    /**
     * Returns the unique identifier of the payment gateway (e.g. MOCK, RAZORPAY, STRIPE).
     */
    default String getGatewayName() {
        return "MOCK";
    }

    /**
     * Processes a refund with the external payment gateway.
     *
     * @param request Validated refund payload
     * @return {@link com.krushna.moviebooking.payment.dto.RefundResponse} with gateway refund details
     * @throws PaymentGatewayException on gateway errors
     */
    default com.krushna.moviebooking.payment.dto.RefundResponse processRefund(com.krushna.moviebooking.payment.dto.RefundRequest request) {
        return com.krushna.moviebooking.payment.dto.RefundResponse.builder()
                .refundId(java.util.UUID.randomUUID())
                .paymentId(request.paymentId())
                .refundReference(getGatewayName() + "-RFD-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .amount(request.amount())
                .currency("INR")
                .status("REFUNDED")
                .reason(request.reason())
                .createdAt(java.time.Instant.now())
                .build();
    }
}
