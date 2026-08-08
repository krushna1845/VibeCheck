package com.krushna.moviebooking.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Filter that appends standard gateway response headers and security headers.
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ResponseHeaderFilter extends OncePerRequestFilter {

    public static final String GATEWAY_TIMESTAMP_HEADER = "X-Gateway-Timestamp";
    public static final String SERVER_HEADER = "X-Gateway-Server";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setHeader(GATEWAY_TIMESTAMP_HEADER, Instant.now().toString());
        response.setHeader(SERVER_HEADER, "MovieBooking-Gateway/1.0");

        // Standard security headers
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");

        filterChain.doFilter(request, response);
    }
}
