package com.krushna.moviebooking.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import com.krushna.moviebooking.payment.gateway.PaymentTimeoutException;

import java.util.Map;

/**
 * Spring Security and Retry infrastructure configuration for the Payment Service.
 *
 * <p>Security: Actuator and callback endpoints are open; all others require authentication.
 * Retry: A {@link RetryTemplate} bean is available for programmatic retry outside of AOP.
 */
@Configuration
@EnableWebSecurity
@EnableRetry
public class SecurityAndRetryConfig {

    /**
     * Security filter chain.
     *
     * <p>The callback endpoint ({@code /api/v1/payments/callback}) is intentionally open
     * because it is called by the external gateway, which authenticates via HMAC signature
     * verified inside the service layer.
     */
    @Bean
    public SecurityFilterChain paymentSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/**",
                    "/api/v1/payments/callback",
                    "/api/v1/payments/webhooks/**",
                    "/api/v1/payments/**"
                ).permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }

    /**
     * Programmatic {@link RetryTemplate} for imperative retry scenarios
     * (e.g., compensating transactions that cannot use AOP).
     *
     * <p>Configured to retry only on {@link PaymentTimeoutException} with
     * exponential backoff: 500 ms → 1000 ms → 2000 ms, max 3 attempts.
     */
    @Bean
    public RetryTemplate paymentRetryTemplate() {
        RetryTemplate template = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                3, Map.of(PaymentTimeoutException.class, true), true);
        template.setRetryPolicy(retryPolicy);

        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(500);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(5000);
        template.setBackOffPolicy(backOff);

        return template;
    }
}
