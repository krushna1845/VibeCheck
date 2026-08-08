package com.krushna.moviebooking.gateway.security;

import com.krushna.moviebooking.gateway.filter.CorrelationIdFilter;
import com.krushna.moviebooking.gateway.filter.GatewayRequestLoggingFilter;
import com.krushna.moviebooking.gateway.filter.ResponseHeaderFilter;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public authentication endpoints
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                // Gateway health, routes, fallback, actuator, and OpenAPI documentation
                .requestMatchers("/gateway/**", "/fallback/**", "/actuator/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Public read-only catalog endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/movies/**", "/api/v1/theatres/**", "/api/v1/shows/**").permitAll()
                // All other endpoints require valid JWT authentication
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
        config.setAllowedOriginPatterns(List.of("*"));
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
