package com.krushna.moviebooking.payment.gateway;

import com.krushna.moviebooking.payment.config.PaymentGatewayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayFactoryTest {

    @Mock
    private PaymentClient mockClient;

    @Mock
    private RazorpayPaymentClient razorpayClient;

    @Mock
    private StripePaymentClient stripeClient;

    private PaymentGatewayProperties properties;
    private PaymentGatewayFactory factory;

    @BeforeEach
    void setUp() {
        properties = new PaymentGatewayProperties();
        Map<String, PaymentClient> map = Map.of(
                "MOCK", mockClient,
                "RAZORPAY", razorpayClient,
                "STRIPE", stripeClient
        );
        factory = new PaymentGatewayFactory(map, properties);
    }

    @Test
    void getPaymentClient_default_returnsMockClient() {
        properties.setProvider("MOCK");
        PaymentClient resolved = factory.getPaymentClient();
        assertThat(resolved).isEqualTo(mockClient);
    }

    @Test
    void getPaymentClient_razorpay_returnsRazorpayClient() {
        PaymentClient resolved = factory.getPaymentClient("RAZORPAY");
        assertThat(resolved).isEqualTo(razorpayClient);
    }

    @Test
    void getPaymentClient_stripe_returnsStripeClient() {
        PaymentClient resolved = factory.getPaymentClient("STRIPE");
        assertThat(resolved).isEqualTo(stripeClient);
    }

    @Test
    void getPaymentClient_unknown_fallbacksToMockClient() {
        PaymentClient resolved = factory.getPaymentClient("UNKNOWN");
        assertThat(resolved).isEqualTo(mockClient);
    }
}
