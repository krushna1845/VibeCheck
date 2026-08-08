package com.krushna.moviebooking.gateway.controller;

import com.krushna.moviebooking.gateway.security.JwtValidator;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gateway controller exposing health, token introspection, circuit breaker statuses, and routing status endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/gateway")
@RequiredArgsConstructor
@Tag(name = "Gateway", description = "API Gateway management, health check, and token introspection endpoints")
public class GatewayController {

    private final JwtValidator jwtValidator;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Operation(summary = "Gateway health check including circuit breaker statuses")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, String> cbStates = new HashMap<>();
        for (CircuitBreaker cb : circuitBreakerRegistry.getAllCircuitBreakers()) {
            cbStates.put(cb.getName(), cb.getState().name());
        }

        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "gateway-service",
                "timestamp", Instant.now().toString(),
                "circuitBreakers", cbStates
        ));
    }

    @Operation(summary = "Introspect a JWT token", description = "Returns claims if token is valid, 401 if invalid")
    @PostMapping("/introspect")
    public ResponseEntity<Map<String, Object>> introspect(@RequestParam String token) {
        if (!jwtValidator.isValid(token)) {
            return ResponseEntity.status(401).body(Map.of("valid", false));
        }

        UUID userId = jwtValidator.getUserId(token);
        String email = jwtValidator.getEmail(token);
        List<String> roles = jwtValidator.getRoles(token);

        log.debug("[Gateway] Token introspected for userId={}", userId);

        return ResponseEntity.ok(Map.of(
                "valid", true,
                "userId", userId,
                "email", email,
                "roles", roles != null ? roles : List.of()
        ));
    }

    @Operation(summary = "List registered downstream service routes")
    @GetMapping("/routes")
    public ResponseEntity<List<Map<String, String>>> routes() {
        return ResponseEntity.ok(List.of(
                Map.of("service", "auth-service",    "url", "http://localhost:8080", "prefix", "/api/v1/auth/**"),
                Map.of("service", "movie-service",   "url", "http://localhost:8081", "prefix", "/api/v1/movies/**"),
                Map.of("service", "theatre-service", "url", "http://localhost:8082", "prefix", "/api/v1/theatres/**"),
                Map.of("service", "show-service",    "url", "http://localhost:8083", "prefix", "/api/v1/shows/**"),
                Map.of("service", "booking-service", "url", "http://localhost:8084", "prefix", "/api/v1/bookings/**"),
                Map.of("service", "payment-service", "url", "http://localhost:8085", "prefix", "/api/v1/payments/**")
        ));
    }
}
