package com.krushna.moviebooking.gateway.controller;

import com.krushna.moviebooking.gateway.service.GatewayMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("FallbackController Unit & Integration Tests")
class FallbackControllerTest {

    @Mock
    private GatewayMetricsService metricsService;

    @InjectMocks
    private FallbackController fallbackController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fallbackController).build();
    }

    @Test
    @DisplayName("GET /fallback/auth returns HTTP 503 with auth-service details")
    void authFallback_ReturnsServiceUnavailable() throws Exception {
        mockMvc.perform(get("/fallback/auth"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.service").value("auth-service"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("GET /fallback/booking returns HTTP 503 with booking-service details")
    void bookingFallback_ReturnsServiceUnavailable() throws Exception {
        mockMvc.perform(get("/fallback/booking"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.service").value("booking-service"));
    }

    @Test
    @DisplayName("GET /fallback/payment returns HTTP 503 with payment-service details")
    void paymentFallback_ReturnsServiceUnavailable() throws Exception {
        mockMvc.perform(get("/fallback/payment"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.service").value("payment-service"));
    }

    @Test
    @DisplayName("GET /fallback/default returns HTTP 503 with gateway-service details")
    void defaultFallback_ReturnsServiceUnavailable() throws Exception {
        mockMvc.perform(get("/fallback/default"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.service").value("gateway-service"));
    }
}
