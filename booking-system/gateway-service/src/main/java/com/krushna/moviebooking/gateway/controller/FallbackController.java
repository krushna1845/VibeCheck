package com.krushna.moviebooking.gateway.controller;

import com.krushna.moviebooking.gateway.service.GatewayMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Controller exposing fallback endpoints for Resilience4j CircuitBreakers and Timeouts.
 * Returns standard JSON responses with HTTP 503 SERVICE_UNAVAILABLE or HTTP 504 GATEWAY_TIMEOUT.
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
@RequiredArgsConstructor
@Tag(name = "Fallback", description = "Resilience4j fallback endpoints for downstream services")
public class FallbackController {

    private final GatewayMetricsService metricsService;

    @Operation(summary = "Auth Service fallback")
    @RequestMapping("/auth")
    public ResponseEntity<Map<String, Object>> authFallback() {
        metricsService.recordFallback("auth-service", "CircuitBreaker/Timeout");
        return buildFallbackResponse("Auth Service is currently unavailable. Please try again shortly.", "auth-service");
    }

    @Operation(summary = "Movie Service fallback")
    @RequestMapping("/movie")
    public ResponseEntity<Map<String, Object>> movieFallback() {
        metricsService.recordFallback("movie-service", "CircuitBreaker/Timeout");
        return buildFallbackResponse("Movie Service is experiencing high load or outage. Please try again shortly.", "movie-service");
    }

    @Operation(summary = "Theatre Service fallback")
    @RequestMapping("/theatre")
    public ResponseEntity<Map<String, Object>> theatreFallback() {
        metricsService.recordFallback("theatre-service", "CircuitBreaker/Timeout");
        return buildFallbackResponse("Theatre Service is temporarily unavailable. Please try again shortly.", "theatre-service");
    }

    @Operation(summary = "Show Service fallback")
    @RequestMapping("/show")
    public ResponseEntity<Map<String, Object>> showFallback() {
        metricsService.recordFallback("show-service", "CircuitBreaker/Timeout");
        return buildFallbackResponse("Show Service is temporarily unavailable. Please try again shortly.", "show-service");
    }

    @Operation(summary = "Booking Service fallback")
    @RequestMapping("/booking")
    public ResponseEntity<Map<String, Object>> bookingFallback() {
        metricsService.recordFallback("booking-service", "CircuitBreaker/Timeout");
        return buildFallbackResponse("Booking Service is currently overloaded. Your request could not be processed at this time.", "booking-service");
    }

    @Operation(summary = "Payment Service fallback")
    @RequestMapping("/payment")
    public ResponseEntity<Map<String, Object>> paymentFallback() {
        metricsService.recordFallback("payment-service", "CircuitBreaker/Timeout");
        return buildFallbackResponse("Payment Service gateway connection timed out or is unavailable. Please check your transaction status.", "payment-service");
    }

    @Operation(summary = "Default generic fallback")
    @RequestMapping("/default")
    public ResponseEntity<Map<String, Object>> defaultFallback() {
        metricsService.recordFallback("unknown-service", "CircuitBreaker/Timeout");
        return buildFallbackResponse("Downstream service is currently unavailable. Please try again later.", "gateway-service");
    }

    private ResponseEntity<Map<String, Object>> buildFallbackResponse(String message, String service) {
        log.warn("[Fallback] Circuit breaker / Timeout triggered fallback for service: {}", service);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", 503,
                "error", "Service Unavailable",
                "message", message,
                "service", service,
                "timestamp", Instant.now().toString()
        ));
    }
}
