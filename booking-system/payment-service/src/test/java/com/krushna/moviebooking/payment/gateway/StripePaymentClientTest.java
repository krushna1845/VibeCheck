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

class StripePaymentClientTest {

    private StripePaymentClient stripePaymentClient;
    private PaymentGatewayProperties properties;

    @BeforeEach
    void setUp() {
        properties = new PaymentGatewayProperties();
        properties.getStripe().setWebhookSecret("whsec_stripe_test");
        stripePaymentClient = new StripePaymentClient(properties);
    }

    @Test
    void initiatePayment_createsStripeCheckoutSession() {
        PaymentRequest request = PaymentRequest.builder()
                .bookingId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .bookingReference("BK55555")
                .idempotencyKey("idem-stripe-1")
                .amount(new BigDecimal("750.00"))
                .currency("INR")
                .paymentMethod(CARD)
                .build();

        PaymentResponse response = stripePaymentClient.initiatePayment(request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("INITIATED");
        assertThat(response.transactionReference()).startsWith("cs_test_");
        assertThat(response.redirectUrl()).contains("checkout.stripe.com");
    }

    @Test
    void processRefund_returnsStripeRefundedResponse() {
        RefundRequest request = RefundRequest.builder()
                .paymentId(UUID.randomUUID())
                .amount(new BigDecimal("300.00"))
                .reason("User change of plan")
                .idempotencyKey("refund-idem-stripe")
                .build();

        RefundResponse response = stripePaymentClient.processRefund(request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("REFUNDED");
        assertThat(response.refundReference()).startsWith("re_");
    }

    @Test
    void verifyWebhookSignature_validStripeHeader_returnsTrue() {
        String rawBody = "{\"id\":\"evt_123\",\"type\":\"payment_intent.succeeded\"}";
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String secret = "whsec_stripe_test";
        String signature = HmacUtils.calculateHmacSha256(timestamp + "." + rawBody, secret);
        String header = "t=" + timestamp + ",v1=" + signature;

        boolean result = stripePaymentClient.verifyWebhookSignature(rawBody, header);
        assertThat(result).isTrue();
    }

    @Test
    void verifyWebhookSignature_invalidStripeHeader_returnsFalse() {
        boolean result = stripePaymentClient.verifyWebhookSignature("{}", "t=123,v1=invalid");
        assertThat(result).isFalse();
    }

    private static final String CARD = "CREDIT_CARD";
}
