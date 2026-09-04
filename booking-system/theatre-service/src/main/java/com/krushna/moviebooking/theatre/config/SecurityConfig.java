package com.krushna.moviebooking.theatre.config;

import com.krushna.moviebooking.common.security.InternalAuthConstants;
import com.krushna.moviebooking.common.security.InternalAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Hardened Security configuration for theatre-service.
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

                // Public read-only catalog endpoints
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/theatres/**",
                        "/api/v1/screens/**",
                        "/api/v1/seats/**",
                        "/api/v1/cities/**").permitAll()

                // Admin catalog mutations require ROLE_ADMIN or ROLE_INTERNAL_SERVICE
                .requestMatchers(HttpMethod.POST,
                        "/api/v1/theatres/**",
                        "/api/v1/screens/**",
                        "/api/v1/seats/**",
                        "/api/v1/cities/**").hasAnyRole("ADMIN", "INTERNAL_SERVICE")
                .requestMatchers(HttpMethod.PUT,
                        "/api/v1/theatres/**",
                        "/api/v1/screens/**",
                        "/api/v1/seats/**",
                        "/api/v1/cities/**").hasAnyRole("ADMIN", "INTERNAL_SERVICE")
                .requestMatchers(HttpMethod.DELETE,
                        "/api/v1/theatres/**",
                        "/api/v1/screens/**",
                        "/api/v1/seats/**",
                        "/api/v1/cities/**").hasAnyRole("ADMIN", "INTERNAL_SERVICE")
                .requestMatchers(HttpMethod.PATCH,
                        "/api/v1/theatres/**",
                        "/api/v1/screens/**",
                        "/api/v1/seats/**",
                        "/api/v1/cities/**").hasAnyRole("ADMIN", "INTERNAL_SERVICE")

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(internalAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
