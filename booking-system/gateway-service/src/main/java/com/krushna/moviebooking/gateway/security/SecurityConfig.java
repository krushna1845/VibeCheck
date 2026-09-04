package com.krushna.moviebooking.gateway.security;

import com.krushna.moviebooking.gateway.filter.CorrelationIdFilter;
import com.krushna.moviebooking.gateway.filter.GatewayRequestLoggingFilter;
import com.krushna.moviebooking.gateway.filter.ResponseHeaderFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayJwtFilter jwtFilter;
    private final CorrelationIdFilter correlationIdFilter;
    private final GatewayRequestLoggingFilter requestLoggingFilter;
    private final ResponseHeaderFilter responseHeaderFilter;

    @Value("${cors.allowed-origins:*}")
    private String corsAllowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"Full authentication is required to access this resource\"}}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"FORBIDDEN\",\"message\":\"Access denied: insufficient permissions\"}}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                // 1. Explicit public authentication endpoints
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()

                // 2. Gateway health, route inspection, fallback endpoints
                .requestMatchers("/gateway/**", "/fallback/**").permitAll()

                // 3. Actuator health & info (restricted to prevent sensitive leakage)
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                // 4. OpenAPI / Swagger documentation (development/local only)
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // 5. Payment webhooks / callbacks (HMAC signature validated at service layer)
                .requestMatchers("/api/v1/payments/callback", "/api/v1/payments/webhooks/**").permitAll()

                // 6. Public read-only catalog endpoints
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/movies/**",
                        "/api/v1/genres/**",
                        "/api/v1/languages/**",
                        "/api/v1/theatres/**",
                        "/api/v1/screens/**",
                        "/api/v1/seats/**",
                        "/api/v1/cities/**",
                        "/api/v1/shows/**").permitAll()

                // 7. Admin-only catalog management mutations (Movies, Theatres, Screens, Seats, Shows)
                .requestMatchers(HttpMethod.POST,
                        "/api/v1/movies/**",
                        "/api/v1/genres/**",
                        "/api/v1/languages/**",
                        "/api/v1/theatres/**",
                        "/api/v1/screens/**",
                        "/api/v1/seats/**",
                        "/api/v1/shows/**",
                        "/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,
                        "/api/v1/movies/**",
                        "/api/v1/genres/**",
                        "/api/v1/languages/**",
                        "/api/v1/theatres/**",
                        "/api/v1/screens/**",
                        "/api/v1/seats/**",
                        "/api/v1/shows/**",
                        "/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE,
                        "/api/v1/movies/**",
                        "/api/v1/genres/**",
                        "/api/v1/languages/**",
                        "/api/v1/theatres/**",
                        "/api/v1/screens/**",
                        "/api/v1/seats/**",
                        "/api/v1/shows/**",
                        "/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH,
                        "/api/v1/movies/**",
                        "/api/v1/genres/**",
                        "/api/v1/languages/**",
                        "/api/v1/theatres/**",
                        "/api/v1/screens/**",
                        "/api/v1/seats/**",
                        "/api/v1/shows/**",
                        "/api/v1/admin/**").hasRole("ADMIN")

                // 8. Protected business APIs (Bookings, Payments, Tickets)
                .requestMatchers("/api/v1/bookings/**", "/api/v1/payments/**", "/api/v1/tickets/**").authenticated()

                // 9. All other requests require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(requestLoggingFilter, CorrelationIdFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(responseHeaderFilter, GatewayJwtFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        if ("*".equals(corsAllowedOrigins.trim())) {
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            config.setAllowedOrigins(Arrays.stream(corsAllowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList());
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-Correlation-ID", "X-Gateway-Timestamp", "Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
