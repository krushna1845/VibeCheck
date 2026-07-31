package com.krushna.moviebooking.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration properties for the external payment gateway integration.
 *
 * <p>Bound from {@code payment.gateway.*} keys in {@code application.yml}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "payment.gateway")
public class PaymentGatewayProperties {

    /** Base URL of the payment gateway checkout (mock or real). */
    private String baseUrl = "https://mock-payment-gateway.internal";

    /** Shared secret used to verify HMAC-SHA256 callback signatures. */
    private String webhookSecret = "default-secret-replace-in-prod";

    /** Timeout in milliseconds for outbound gateway calls. */
    private long timeoutMs = 5000;

    /** Retry configuration for {@link com.krushna.moviebooking.payment.gateway.PaymentClientImpl}. */
    private Retry retry = new Retry();

    @Getter
    @Setter
    public static class Retry {
        private int maxAttempts = 3;
        private long initialDelayMs = 500;
        private double multiplier = 2.0;
        private long maxDelayMs = 5000;
    }
}
