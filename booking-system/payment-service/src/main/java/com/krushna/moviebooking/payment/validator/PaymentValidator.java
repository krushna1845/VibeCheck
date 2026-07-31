package com.krushna.moviebooking.payment.validator;

import com.krushna.moviebooking.payment.dto.PaymentCallback;
import com.krushna.moviebooking.payment.dto.PaymentRequest;
import com.krushna.moviebooking.payment.exception.InvalidPaymentRequestException;
import com.krushna.moviebooking.payment.exception.InvalidSignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Set;

/**
 * Validates inbound {@link PaymentRequest} and {@link PaymentCallback} payloads.
 *
 * <p><b>PaymentRequest validation</b>:
 * <ul>
 *   <li>Amount must be positive.</li>
 *   <li>Currency must be a supported 3-character ISO code.</li>
 *   <li>Idempotency key must be non-blank and within size limits.</li>
 * </ul>
 *
 * <p><b>PaymentCallback validation</b>:
 * <ul>
 *   <li>HMAC-SHA256 signature must match the shared webhook secret.</li>
 *   <li>Gateway status must be a known value.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentValidator {

    private static final Set<String> SUPPORTED_CURRENCIES   = Set.of("INR", "USD", "EUR", "GBP");
    private static final Set<String> VALID_GATEWAY_STATUSES = Set.of("SUCCESS", "FAILED", "PENDING");
    private static final String      HMAC_ALGORITHM          = "HmacSHA256";

    private final com.krushna.moviebooking.payment.config.PaymentGatewayProperties gatewayProperties;

    /**
     * Validates a {@link PaymentRequest} for business correctness.
     *
     * @param request Incoming payment request
     * @throws InvalidPaymentRequestException on validation failure
     */
    public void validatePaymentRequest(PaymentRequest request) {
        log.debug("[PaymentValidator] Validating payment request for bookingRef={}", request.bookingReference());

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentRequestException("Payment amount must be greater than zero.");
        }

        String currency = request.currency() != null ? request.currency().toUpperCase() : "INR";
        if (!SUPPORTED_CURRENCIES.contains(currency)) {
            throw new InvalidPaymentRequestException(
                    "Unsupported currency: " + currency + ". Supported: " + SUPPORTED_CURRENCIES);
        }

        if (request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
            throw new InvalidPaymentRequestException("Idempotency key must not be blank.");
        }

        if (request.idempotencyKey().length() > 255) {
            throw new InvalidPaymentRequestException("Idempotency key must be at most 255 characters.");
        }

        if (request.bookingReference() == null || request.bookingReference().isBlank()) {
            throw new InvalidPaymentRequestException("Booking reference must not be blank.");
        }

        log.debug("[PaymentValidator] Payment request valid for bookingRef={}", request.bookingReference());
    }

    /**
     * Validates a gateway {@link PaymentCallback} payload, including HMAC-SHA256 signature.
     *
     * @param callback Inbound callback from gateway
     * @throws InvalidSignatureException      when HMAC verification fails
     * @throws InvalidPaymentRequestException when status value is unrecognised
     */
    public void validateCallback(PaymentCallback callback) {
        log.debug("[PaymentValidator] Validating callback signature for txnRef={}", callback.transactionReference());

        verifyHmacSignature(callback);

        String status = callback.gatewayStatus() != null ? callback.gatewayStatus().toUpperCase() : "";
        if (!VALID_GATEWAY_STATUSES.contains(status)) {
            throw new InvalidPaymentRequestException(
                    "Unknown gateway status: " + callback.gatewayStatus()
                            + ". Expected one of: " + VALID_GATEWAY_STATUSES);
        }

        log.debug("[PaymentValidator] Callback valid for txnRef={}", callback.transactionReference());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void verifyHmacSignature(PaymentCallback callback) {
        try {
            String payload = callback.transactionReference()
                    + "|" + callback.gatewayStatus()
                    + "|" + callback.gatewayName();

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    gatewayProperties.getWebhookSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM));
            byte[] computed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computed);

            if (!computedHex.equalsIgnoreCase(callback.signature())) {
                log.warn("[PaymentValidator] Signature mismatch for txnRef={}", callback.transactionReference());
                throw new InvalidSignatureException(
                        "Callback signature verification failed for txnRef: " + callback.transactionReference());
            }
        } catch (InvalidSignatureException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[PaymentValidator] HMAC computation error: {}", ex.getMessage());
            throw new InvalidSignatureException("Failed to compute HMAC for callback verification", ex);
        }
    }
}
