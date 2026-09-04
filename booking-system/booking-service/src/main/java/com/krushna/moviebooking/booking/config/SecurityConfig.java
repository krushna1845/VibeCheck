package com.krushna.moviebooking.booking.config;

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
 * Hardened Security configuration for booking-service.
 * Unsafe .anyRequest().permitAll() has been removed.
 * Enforces authentication via InternalAuthFilter and role-based access control.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${internal.security.secret:" + InternalAuthConstants.DEFAULT_INTERNAL_SECRET + "}")
    private String internalSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        InternalAuthFilter internalAuthFilter = new InternalAuthFilter(internalSecret);

        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Actuator health & info
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                // Swagger UI & OpenAPI (development/local)
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // Permit WebSocket handshake and SockJS transport endpoints
                .requestMatchers("/ws-seat-availability/**").permitAll()

                // Admin endpoints require ROLE_ADMIN or ROLE_INTERNAL_SERVICE
                .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "INTERNAL_SERVICE")

                // Booking business APIs require authentication
                .requestMatchers("/api/v1/bookings/**").authenticated()

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(internalAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
