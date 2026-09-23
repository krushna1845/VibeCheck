package com.krushna.moviebooking.gateway.filter;

import com.krushna.moviebooking.gateway.redis.GatewayRedisRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private GatewayRedisRepository gatewayRedisRepository;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private RateLimitingFilter rateLimitingFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("should permit request when client is within rate limit")
    void shouldAllowRequestWhenWithinRateLimit() throws ServletException, IOException {
        request.setRequestURI("/api/v1/movies");
        request.setRemoteAddr("192.168.1.100");

        when(gatewayRedisRepository.isWithinRateLimit("ip:192.168.1.100")).thenReturn(true);

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("should return 429 Too Many Requests when rate limit is exceeded")
    void shouldBlockRequestWhenRateLimitExceeded() throws ServletException, IOException {
        request.setRequestURI("/api/v1/bookings");
        request.setRemoteAddr("10.0.0.5");

        when(gatewayRedisRepository.isWithinRateLimit("ip:10.0.0.5")).thenReturn(false);

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        assertThat(response.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("should bypass rate limiting for actuator and swagger endpoints")
    void shouldBypassRateLimitingForExcludedEndpoints() throws ServletException, IOException {
        request.setRequestURI("/actuator/health");

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(gatewayRedisRepository, never()).isWithinRateLimit(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("should rate limit by userId when request is authenticated")
    void shouldRateLimitByUserIdWhenAuthenticated() throws ServletException, IOException {
        UUID userId = UUID.randomUUID();
        var auth = new UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        request.setRequestURI("/api/v1/bookings");
        when(gatewayRedisRepository.isWithinRateLimit(eq("user:" + userId))).thenReturn(true);

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(gatewayRedisRepository).isWithinRateLimit("user:" + userId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("should use X-Forwarded-For IP when header is present for anonymous request")
    void shouldUseXForwardedForWhenPresent() throws ServletException, IOException {
        request.setRequestURI("/api/v1/movies");
        request.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18");
        request.setRemoteAddr("10.0.0.1");

        when(gatewayRedisRepository.isWithinRateLimit("ip:203.0.113.195")).thenReturn(true);

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(gatewayRedisRepository).isWithinRateLimit("ip:203.0.113.195");
        verify(filterChain).doFilter(request, response);
    }
}
