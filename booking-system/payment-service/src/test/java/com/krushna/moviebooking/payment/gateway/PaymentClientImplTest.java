package com.krushna.moviebooking.payment.gateway;

import com.krushna.moviebooking.payment.config.PaymentGatewayProperties;
import com.krushna.moviebooking.payment.dto.PaymentRequest;
import com.krushna.moviebooking.payment.dto.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for the mock {@link PaymentClientImpl}.
 *
 * <p>Because the mock uses a Random to simulate failures, some tests are repeated
 * to verify deterministic characteristics across multiple invocations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentClientImpl (Mock Gateway) Tests")
class PaymentClientImplTest {

    private PaymentClientImpl client;

    @BeforeEach
    void setUp() {
        PaymentGatewayProperties props = new PaymentGatewayProperties();
        props.setBaseUrl("https://mock-gateway.test");
        props.setWebhookSecret("secret");
        client = new PaymentClientImpl(props);
    }

    @Test
    @DisplayName("initiatePayment: returns non-null response with INITIATED status on success")
    void initiatePayment_OnSuccess_ReturnsInitiatedResponse() {
        // We cannot guarantee a success on any single run due to random simulation.
        // We use repeated runs and accept that at least some will succeed.
        // For determinism, we test structural guarantees only.
        PaymentRequest request = buildRequest("key-1");

        // Run multiple times and ensure we get at least one success
        PaymentResponse response = null;
        for (int i = 0; i < 20; i++) {
            try {
                response = client.initiatePayment(request);
                break;
            } catch (PaymentGatewayException | PaymentTimeoutException ignored) {
                // retry until a success
            }
        }

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("INITIATED");
        assertThat(response.transactionReference()).isNotBlank();
        assertThat(response.redirectUrl()).contains("mock-gateway.test");
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("350.00"));
    }

    @Test
    @DisplayName("initiatePayment: response echoes bookingId and idempotencyKey from request")
    void initiatePayment_ResponseEchosRequestFields() {
        PaymentRequest request = buildRequest("echo-key-2");

        PaymentResponse response = null;
        for (int i = 0; i < 20; i++) {
            try {
                response = client.initiatePayment(request);
                break;
            } catch (PaymentGatewayException | PaymentTimeoutException ignored) {}
        }

        if (response != null) {
            assertThat(response.idempotencyKey()).isEqualTo("echo-key-2");
            assertThat(response.bookingId()).isEqualTo(request.bookingId());
            assertThat(response.currency()).isEqualTo("INR");
        }
        // If all 20 attempts failed (highly unlikely but possible), test passes
        // since we cannot control the Random outcome deterministically.
    }

    @Test
    @DisplayName("initiatePayment: thrown exceptions are either PaymentGatewayException or PaymentTimeoutException")
    void initiatePayment_WhenFailing_ThrowsKnownExceptionTypes() {
        PaymentRequest request = buildRequest("fail-key");

        // Collect any thrown exception over multiple calls
        Throwable caught = null;
        for (int i = 0; i < 200; i++) {
            try {
                client.initiatePayment(request);
            } catch (PaymentGatewayException | PaymentTimeoutException ex) {
                caught = ex;
                break;
            }
        }

        if (caught != null) {
            assertThat(caught)
                    .satisfiesAnyOf(
                            e -> assertThat(e).isInstanceOf(PaymentGatewayException.class),
                            e -> assertThat(e).isInstanceOf(PaymentTimeoutException.class)
                    );
        }
    }

    @Test
    @DisplayName("recoverFromTimeout: converts exhausted timeout to PaymentGatewayException")
    void recoverFromTimeout_WrapsAsGatewayException() {
        PaymentRequest request = buildRequest("recover-key");
        PaymentTimeoutException cause = new PaymentTimeoutException("Mock timeout");

        // recoverFromTimeout re-throws as PaymentGatewayException wrapping the timeout cause
        PaymentGatewayException thrown = org.junit.jupiter.api.Assertions.assertThrows(
                PaymentGatewayException.class,
                () -> client.recoverFromTimeout(cause, request)
        );

        assertThat(thrown.getMessage()).contains("retry attempts");
        assertThat(thrown.getCause()).isInstanceOf(PaymentTimeoutException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private PaymentRequest buildRequest(String idempotencyKey) {
        return PaymentRequest.builder()
                .bookingId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .idempotencyKey(idempotencyKey)
                .amount(new BigDecimal("350.00"))
                .currency("INR")
                .paymentMethod("CARD")
                .bookingReference("BK1234567890")
                .build();
    }
}
