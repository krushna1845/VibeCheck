package com.krushna.moviebooking.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("CorrelationIdFilter Unit Tests")
class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("generates new UUID correlation ID when header is missing")
    void doFilterInternal_GeneratesCorrelationId_WhenMissing() throws ServletException, IOException {
        filter.doFilterInternal(request, response, filterChain);

        String correlationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).isNotNull().isNotEmpty();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("preserves existing correlation ID when header is present")
    void doFilterInternal_PreservesExistingCorrelationId() throws ServletException, IOException {
        String existingId = "test-correlation-12345";
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId);

        filter.doFilterInternal(request, response, filterChain);

        String correlationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).isEqualTo(existingId);
        verify(filterChain).doFilter(request, response);
    }
}
