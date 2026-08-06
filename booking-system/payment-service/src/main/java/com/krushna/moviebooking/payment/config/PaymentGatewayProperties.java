package com.krushna.moviebooking.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration properties for external payment gateways (Mock, Razorpay, Stripe).
 *
 * <p>Bound from {@code payment.gateway.*} keys in {@code application.yml}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "payment.gateway")
public class PaymentGatewayProperties {

    /** Gateway provider name: MOCK, RAZORPAY, STRIPE */
    private String provider = "MOCK";

    /** Base URL of the mock payment gateway checkout. */
    private String baseUrl = "https://mock-payment-gateway.internal";

    /** Shared secret used to verify mock HMAC-SHA256 callback signatures. */
    private String webhookSecret = "default-secret-replace-in-prod";

    /** Timeout in milliseconds for outbound gateway calls. */
    private long timeoutMs = 5000;

    /** Razorpay gateway configuration */
    private Razorpay razorpay = new Razorpay();

    /** Stripe gateway configuration */
    private Stripe stripe = new Stripe();

    /** Retry configuration for PaymentClients. */
    private Retry retry = new Retry();

    @Getter
    @Setter
    public static class Razorpay {
        private String keyId = "rzp_test_placeholder_key";
        private String keySecret = "rzp_test_placeholder_secret";
        private String webhookSecret = "rzp_test_webhook_secret";
        private String baseUrl = "https://api.razorpay.com/v1";
    }

    @Getter
    @Setter
    public static class Stripe {
        private String apiKey = "sk_test_placeholder_key";
        private String webhookSecret = "whsec_test_webhook_secret";
        private String baseUrl = "https://api.stripe.com/v1";
    }

    @Getter
    @Setter
    public static class Retry {
        private int maxAttempts = 3;
        private long initialDelayMs = 500;
        private double multiplier = 2.0;
        private long maxDelayMs = 5000;
    }
}
