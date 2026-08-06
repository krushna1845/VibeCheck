package com.krushna.moviebooking.payment.gateway;

import com.krushna.moviebooking.payment.config.PaymentGatewayProperties;
import com.krushna.moviebooking.payment.dto.PaymentRequest;
import com.krushna.moviebooking.payment.dto.PaymentResponse;
import com.krushna.moviebooking.payment.dto.RefundRequest;
import com.krushna.moviebooking.payment.dto.RefundResponse;
import com.krushna.moviebooking.payment.security.HmacUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RazorpayPaymentClientTest {

    private RazorpayPaymentClient razorpayPaymentClient;
    private PaymentGatewayProperties properties;

    @BeforeEach
    void setUp() {
        properties = new PaymentGatewayProperties();
        properties.getRazorpay().setWebhookSecret("test_webhook_secret");
        razorpayPaymentClient = new RazorpayPaymentClient(properties);
    }

    @Test
    void initiatePayment_createsOrderAndRedirectUrl() {
        PaymentRequest request = PaymentRequest.builder()
                .bookingId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .bookingReference("BK12345")
                .idempotencyKey("idem-123")
                .amount(new BigDecimal("500.00"))
                .currency("INR")
                .paymentMethod("CREDIT_CARD")
                .build();

        PaymentResponse response = razorpayPaymentClient.initiatePayment(request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("INITIATED");
        assertThat(response.transactionReference()).startsWith("order_");
        assertThat(response.redirectUrl()).contains("checkout?order_id=");
    }

    @Test
    void processRefund_returnsRefundedResponse() {
        RefundRequest request = RefundRequest.builder()
                .paymentId(UUID.randomUUID())
                .amount(new BigDecimal("200.00"))
                .reason("Customer cancellation")
                .idempotencyKey("refund-idem-1")
                .build();

        RefundResponse response = razorpayPaymentClient.processRefund(request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("REFUNDED");
        assertThat(response.refundReference()).startsWith("rfnd_");
    }

    @Test
    void verifyWebhookSignature_validSignature_returnsTrue() {
        String orderId = "order_123456";
        String paymentId = "pay_789012";
        String secret = "test_webhook_secret";
        String payload = orderId + "|" + paymentId;
        String validSig = HmacUtils.calculateHmacSha256(payload, secret);

        boolean result = razorpayPaymentClient.verifyWebhookSignature(orderId, paymentId, validSig);
        assertThat(result).isTrue();
    }

    @Test
    void verifyWebhookSignature_invalidSignature_returnsFalse() {
        boolean result = razorpayPaymentClient.verifyWebhookSignature("order_123", "pay_456", "invalid_sig");
        assertThat(result).isFalse();
    }
}
