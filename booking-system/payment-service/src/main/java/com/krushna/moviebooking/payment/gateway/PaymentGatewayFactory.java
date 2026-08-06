package com.krushna.moviebooking.payment.gateway;

import com.krushna.moviebooking.payment.config.PaymentGatewayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Factory for resolving {@link PaymentClient} instances dynamically based on configuration or gateway name.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentGatewayFactory {

    private final Map<String, PaymentClient> paymentClients;
    private final PaymentGatewayProperties gatewayProperties;

    /**
     * Resolves the {@link PaymentClient} configured by {@code payment.gateway.provider} property.
     *
     * @return Configured {@link PaymentClient}
     */
    public PaymentClient getPaymentClient() {
        String configuredProvider = gatewayProperties.getProvider();
        return getPaymentClient(configuredProvider);
    }

    /**
     * Resolves a specific {@link PaymentClient} implementation by provider name.
     *
     * @param providerName Gateway provider name (e.g. MOCK, RAZORPAY, STRIPE)
     * @return Corresponding {@link PaymentClient}
     * @throws PaymentGatewayException if requested provider is unknown
     */
    public PaymentClient getPaymentClient(String providerName) {
        if (providerName == null || providerName.trim().isEmpty()) {
            providerName = "MOCK";
        }
        String key = providerName.trim().toUpperCase();
        PaymentClient client = paymentClients.get(key);

        if (client == null) {
            log.error("[PaymentGatewayFactory] Unknown payment gateway provider requested: '{}'. Falling back to MOCK.", key);
            client = paymentClients.get("MOCK");
        }

        if (client == null) {
            throw new PaymentGatewayException("No payment gateway implementation available for provider: " + key);
        }

        log.debug("[PaymentGatewayFactory] Resolved payment client: '{}'", key);
        return client;
    }
}
