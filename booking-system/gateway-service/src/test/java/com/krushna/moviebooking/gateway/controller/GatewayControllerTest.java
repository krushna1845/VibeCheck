package com.krushna.moviebooking.gateway.controller;

import com.krushna.moviebooking.gateway.security.JwtValidator;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("GatewayController Unit & Integration Tests")
class GatewayControllerTest {

    @Mock
    private JwtValidator jwtValidator;

    private CircuitBreakerRegistry circuitBreakerRegistry;
    private GatewayController gatewayController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        // Register standard circuit breakers
        circuitBreakerRegistry.circuitBreaker("authService");
        circuitBreakerRegistry.circuitBreaker("bookingService");

        gatewayController = new GatewayController(jwtValidator, circuitBreakerRegistry);
        mockMvc = MockMvcBuilders.standaloneSetup(gatewayController).build();
    }

    @Test
    @DisplayName("GET /gateway/health returns HTTP 200 with status UP and circuit breaker map")
    void health_ReturnsOk() throws Exception {
        mockMvc.perform(get("/gateway/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("gateway-service"))
                .andExpect(jsonPath("$.circuitBreakers.authService").value("CLOSED"))
                .andExpect(jsonPath("$.circuitBreakers.bookingService").value("CLOSED"));
    }

    @Test
    @DisplayName("GET /gateway/routes returns HTTP 200 with registered downstream service routes")
    void routes_ReturnsRouteList() throws Exception {
        mockMvc.perform(get("/gateway/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].service").value("auth-service"))
                .andExpect(jsonPath("$[4].service").value("booking-service"));
    }

    @Test
    @DisplayName("POST /gateway/introspect returns 401 for invalid token")
    void introspect_InvalidToken_Returns401() throws Exception {
        when(jwtValidator.isValid("invalid-token")).thenReturn(false);

        mockMvc.perform(post("/gateway/introspect").param("token", "invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    @DisplayName("POST /gateway/introspect returns 200 with claims for valid token")
    void introspect_ValidToken_ReturnsClaims() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtValidator.isValid("valid-token")).thenReturn(true);
        when(jwtValidator.getUserId("valid-token")).thenReturn(userId);
        when(jwtValidator.getEmail("valid-token")).thenReturn("user@example.com");
        when(jwtValidator.getRoles("valid-token")).thenReturn(List.of("ROLE_CUSTOMER"));

        mockMvc.perform(post("/gateway/introspect").param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_CUSTOMER"));
    }
}
