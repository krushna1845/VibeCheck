package com.krushna.moviebooking.notification.config;

import com.krushna.moviebooking.common.security.InternalAuthConstants;
import com.krushna.moviebooking.common.security.InternalAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for notification-service.
 * All ticket APIs require authentication via the internal perimeter secret.
 * Only actuator health/info and Swagger endpoints are public.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${internal.security.secret:" + InternalAuthConstants.DEFAULT_INTERNAL_SECRET + "}")
    private String internalSecret;

    @Bean
    public SecurityFilterChain notificationSecurityFilterChain(HttpSecurity http) throws Exception {
        InternalAuthFilter internalAuthFilter = new InternalAuthFilter(internalSecret);

        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Actuator health & info
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                // Swagger UI & OpenAPI (development/local)
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // Ticket retrieval APIs require authentication
                .requestMatchers("/api/v1/tickets/**").authenticated()

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(internalAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
