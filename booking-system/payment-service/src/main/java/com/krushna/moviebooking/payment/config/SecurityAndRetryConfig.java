package com.krushna.moviebooking.payment.config;

import com.krushna.moviebooking.common.security.InternalAuthConstants;
import com.krushna.moviebooking.common.security.InternalAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.krushna.moviebooking.payment.gateway.PaymentTimeoutException;

import java.util.Map;

/**
 * Spring Security and Retry infrastructure configuration for the Payment Service.
 *
 * <p>Security: Hardened perimeter configuration.
 * Only payment callback and webhook endpoints are public (HMAC-verified at service layer).
 * All other payment APIs require authentication via {@link InternalAuthFilter}.
 * Retry: A {@link RetryTemplate} bean is available for programmatic retry outside of AOP.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableRetry
public class SecurityAndRetryConfig {

    @Value("${internal.security.secret:" + InternalAuthConstants.DEFAULT_INTERNAL_SECRET + "}")
    private String internalSecret;

    /**
     * Hardened payment security filter chain.
     *
     * <p>Public endpoints:
     * <ul>
     *   <li>{@code /api/v1/payments/callback} — external payment gateway redirect, HMAC-verified</li>
     *   <li>{@code /api/v1/payments/webhooks/**} — external webhook events, HMAC-verified</li>
     *   <li>{@code /actuator/health}, {@code /actuator/info} — liveness/readiness probes</li>
     *   <li>Swagger UI — development/local only</li>
     * </ul>
     * All other payment endpoints require a valid perimeter secret or authenticated JWT.
     */
    @Bean
    public SecurityFilterChain paymentSecurityFilterChain(HttpSecurity http) throws Exception {
        InternalAuthFilter internalAuthFilter = new InternalAuthFilter(internalSecret);

        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Actuator health & info
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                // Swagger UI & OpenAPI (development/local)
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // External payment gateway callback & webhooks (HMAC-verified at service layer)
                .requestMatchers("/api/v1/payments/callback", "/api/v1/payments/webhooks/**").permitAll()

                // All other payment APIs require authentication
                .requestMatchers("/api/v1/payments/**").authenticated()

                // Catch-all
                .anyRequest().authenticated()
            )
            .addFilterBefore(internalAuthFilter, UsernamePasswordAuthenticationFilter.class);

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
