package com.krushna.moviebooking.gateway;

import com.krushna.moviebooking.gateway.controller.FallbackController;
import com.krushna.moviebooking.gateway.controller.ProxyController;
import com.krushna.moviebooking.gateway.service.GatewayMetricsService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Gateway Retry Policy & Idempotency Hardening Tests")
class GatewayRetryPolicyTest {

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
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .build();
        circuitBreakerRegistry = CircuitBreakerRegistry.of(cbConfig);

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(10))
                .retryExceptions(ResourceAccessException.class)
                .build();
        retryRegistry = RetryRegistry.of(retryConfig);

        fallbackController = new FallbackController(metricsService);
        proxyController = new ProxyController(
                restTemplate,
                metricsService,
                circuitBreakerRegistry,
                retryRegistry,
                fallbackController
        );

        mockMvc = MockMvcBuilders.standaloneSetup(proxyController).build();
    }

    @Test
    @DisplayName("TEST 1: GET timeout -> safe retry executes up to 3 times")
    void getTimeout_SafeRetry_RetriesThreeTimes() throws Exception {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                .thenThrow(new ResourceAccessException("Read timed out", new SocketTimeoutException("Read timed out")));

        mockMvc.perform(get("/api/v1/movies/123"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.service").value("movie-service"));

        // GET request is safe/idempotent: verified to be retried 3 times
        verify(restTemplate, times(3)).exchange(
                any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)
        );
    }

    @Test
    @DisplayName("TEST 2: POST booking timeout -> no blind retry (executed exactly once)")
    void postBookingTimeout_NoBlindRetry_ExecutesExactlyOnce() throws Exception {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(byte[].class)))
                .thenThrow(new ResourceAccessException("Socket timed out", new SocketTimeoutException("Socket timed out")));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"showId\":\"550e8400-e29b-41d4-a716-446655440000\",\"showSeatIds\":[\"550e8400-e29b-41d4-a716-446655440001\"]}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.service").value("booking-service"));

        // POST /bookings must NEVER be blindly retried: verified called exactly 1 time
        verify(restTemplate, times(1)).exchange(
                any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(byte[].class)
        );
    }

    @Test
    @DisplayName("TEST 3: POST payments timeout -> no blind retry (executed exactly once)")
    void postPaymentTimeout_NoBlindRetry_ExecutesExactlyOnce() throws Exception {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(byte[].class)))
                .thenThrow(new ResourceAccessException("Connection timed out", new SocketTimeoutException("Connection timed out")));

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":\"550e8400-e29b-41d4-a716-446655440000\",\"amount\":500.00,\"idempotencyKey\":\"idem-123\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.service").value("payment-service"));

        // POST /payments must NEVER be blindly retried: verified called exactly 1 time
        verify(restTemplate, times(1)).exchange(
                any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(byte[].class)
        );
    }

    @Test
    @DisplayName("TEST 4: POST refund timeout -> no blind retry (executed exactly once)")
    void postRefundTimeout_NoBlindRetry_ExecutesExactlyOnce() throws Exception {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(byte[].class)))
                .thenThrow(new ResourceAccessException("Socket timed out", new SocketTimeoutException("Socket timed out")));

        mockMvc.perform(post("/api/v1/payments/550e8400-e29b-41d4-a716-446655440000/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":500.00,\"reason\":\"Customer request\",\"idempotencyKey\":\"refund-123\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.service").value("payment-service"));

        // POST refund must NEVER be blindly retried: verified called exactly 1 time
        verify(restTemplate, times(1)).exchange(
                any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(byte[].class)
        );
    }

    @Test
    @DisplayName("TEST 5: Circuit Breaker tracks failures on POST without retrying and opens after threshold")
    void circuitBreakerTracksPostFailuresWithoutRetry() throws Exception {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(byte[].class)))
                .thenThrow(new RuntimeException("Internal Server Error"));

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("bookingService");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Perform 5 POST requests (each called once without retry)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isServiceUnavailable());
        }

        // Each call executed exactly 1 time (5 total)
        verify(restTemplate, times(5)).exchange(
                any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(byte[].class)
        );

        // Circuit breaker must now be OPEN due to 100% failure rate
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }
}
