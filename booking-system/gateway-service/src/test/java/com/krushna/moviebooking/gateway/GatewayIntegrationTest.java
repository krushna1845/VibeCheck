package com.krushna.moviebooking.gateway;

import com.krushna.moviebooking.gateway.controller.FallbackController;
import com.krushna.moviebooking.gateway.controller.ProxyController;
import com.krushna.moviebooking.gateway.filter.CorrelationIdFilter;
import com.krushna.moviebooking.gateway.filter.ResponseHeaderFilter;
import com.krushna.moviebooking.gateway.service.GatewayMetricsService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Gateway Proxy & Reverse Routing Integration Tests")
class GatewayIntegrationTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private GatewayMetricsService metricsService;

    private CircuitBreakerRegistry circuitBreakerRegistry;
    private RetryRegistry retryRegistry;
    private FallbackController fallbackController;
    private ProxyController proxyController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        retryRegistry = RetryRegistry.ofDefaults();
        fallbackController = new FallbackController(metricsService);
        proxyController = new ProxyController(
                restTemplate,
                metricsService,
                circuitBreakerRegistry,
                retryRegistry,
                fallbackController);

        mockMvc = MockMvcBuilders.standaloneSetup(proxyController)
                .addFilters(new CorrelationIdFilter(), new ResponseHeaderFilter())
                .build();
    }

    @Test
    @DisplayName("proxy request forwards to downstream service and appends Correlation ID & Gateway Headers")
    void proxyRequest_SuccessfulForwarding() throws Exception {
        byte[] responseBody = "{\"status\":\"OK\",\"movies\":[]}".getBytes();
        ResponseEntity<byte[]> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/movies/active")
                        .header("X-Correlation-ID", "custom-correlation-999"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", "custom-correlation-999"))
                .andExpect(header().exists(ResponseHeaderFilter.GATEWAY_TIMESTAMP_HEADER))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(content().string("{\"status\":\"OK\",\"movies\":[]}"));
    }

    @Test
    @DisplayName("proxy request triggers fallback response when downstream service throws Exception")
    void proxyRequest_TriggersFallbackOnDownstreamError() throws Exception {
        when(restTemplate.exchange(any(URI.class), any(), any(), eq(byte[].class)))
                .thenThrow(new RuntimeException("Downstream connection refused"));

        mockMvc.perform(get("/api/v1/bookings/123"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.service").value("booking-service"));
    }
}
