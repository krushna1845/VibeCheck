package com.krushna.moviebooking.payment.validator;

import com.krushna.moviebooking.payment.config.PaymentGatewayProperties;
import com.krushna.moviebooking.payment.dto.PaymentCallback;
import com.krushna.moviebooking.payment.dto.PaymentRequest;
import com.krushna.moviebooking.payment.exception.InvalidPaymentRequestException;
import com.krushna.moviebooking.payment.exception.InvalidSignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link PaymentValidator}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentValidator Unit Tests")
class PaymentValidatorTest {

    private PaymentGatewayProperties gatewayProperties;
    private PaymentValidator          validator;

    private static final String WEBHOOK_SECRET = "test-webhook-secret";

    @BeforeEach
    void setUp() {
        gatewayProperties = new PaymentGatewayProperties();
        gatewayProperties.setWebhookSecret(WEBHOOK_SECRET);
        validator = new PaymentValidator(gatewayProperties);
    }

    // -------------------------------------------------------------------------
    // validatePaymentRequest
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("validatePaymentRequest: passes for valid request")
    void validatePaymentRequest_Valid_NoException() {
        PaymentRequest request = validRequest();
        assertThatNoException().isThrownBy(() -> validator.validatePaymentRequest(request));
    }

    @Test
    @DisplayName("validatePaymentRequest: throws for zero amount")
    void validatePaymentRequest_ZeroAmount_Throws() {
        PaymentRequest request = PaymentRequest.builder()
                .bookingId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .idempotencyKey("key-1")
                .amount(BigDecimal.ZERO)
                .currency("INR")
                .bookingReference("BK1234567890")
                .build();

        assertThatThrownBy(() -> validator.validatePaymentRequest(request))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("amount must be greater than zero");
    }

    @Test
    @DisplayName("validatePaymentRequest: throws for unsupported currency")
    void validatePaymentRequest_UnsupportedCurrency_Throws() {
        PaymentRequest request = PaymentRequest.builder()
                .bookingId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .idempotencyKey("key-2")
                .amount(new BigDecimal("100.00"))
                .currency("XYZ")
                .bookingReference("BK1234567890")
                .build();

        assertThatThrownBy(() -> validator.validatePaymentRequest(request))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("Unsupported currency");
    }

    @Test
    @DisplayName("validatePaymentRequest: throws for blank idempotency key")
    void validatePaymentRequest_BlankIdempotencyKey_Throws() {
        PaymentRequest request = PaymentRequest.builder()
                .bookingId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .idempotencyKey("   ")
                .amount(new BigDecimal("100.00"))
                .currency("INR")
                .bookingReference("BK1234567890")
                .build();

        assertThatThrownBy(() -> validator.validatePaymentRequest(request))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("Idempotency key must not be blank");
    }

    // -------------------------------------------------------------------------
    // validateCallback
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("validateCallback: passes for valid SUCCESS callback with correct signature")
    void validateCallback_ValidSuccess_NoException() throws Exception {
        String txnRef = "TXN-ABC123";
        String status = "SUCCESS";
        String gateway = "MOCK_GATEWAY";
        String sig = computeHmac(txnRef + "|" + status + "|" + gateway, WEBHOOK_SECRET);

        PaymentCallback callback = PaymentCallback.builder()
                .transactionReference(txnRef)
                .gatewayStatus(status)
                .gatewayName(gateway)
                .signature(sig)
                .build();

        assertThatNoException().isThrownBy(() -> validator.validateCallback(callback));
    }

    @Test
    @DisplayName("validateCallback: throws InvalidSignatureException for wrong signature")
    void validateCallback_WrongSignature_Throws() {
        PaymentCallback callback = PaymentCallback.builder()
                .transactionReference("TXN-XYZ")
                .gatewayStatus("SUCCESS")
                .gatewayName("MOCK_GATEWAY")
                .signature("wrong-signature")
                .build();

        assertThatThrownBy(() -> validator.validateCallback(callback))
                .isInstanceOf(InvalidSignatureException.class);
    }

    @Test
    @DisplayName("validateCallback: throws for unknown gateway status even with valid signature")
    void validateCallback_UnknownStatus_Throws() throws Exception {
        String txnRef = "TXN-DEF456";
        String status = "UNKNOWN_STATUS";
        String gateway = "MOCK_GATEWAY";
        String sig = computeHmac(txnRef + "|" + status + "|" + gateway, WEBHOOK_SECRET);

        PaymentCallback callback = PaymentCallback.builder()
                .transactionReference(txnRef)
                .gatewayStatus(status)
                .gatewayName(gateway)
                .signature(sig)
                .build();

        assertThatThrownBy(() -> validator.validateCallback(callback))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("Unknown gateway status");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private PaymentRequest validRequest() {
        return PaymentRequest.builder()
                .bookingId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .idempotencyKey("valid-key-001")
                .amount(new BigDecimal("500.00"))
                .currency("INR")
                .paymentMethod("UPI")
                .bookingReference("BK0987654321")
                .build();
    }

    private String computeHmac(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(bytes);
    }
}
