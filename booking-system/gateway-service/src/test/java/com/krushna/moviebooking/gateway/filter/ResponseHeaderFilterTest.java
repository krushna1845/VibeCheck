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

@DisplayName("ResponseHeaderFilter Unit Tests")
class ResponseHeaderFilterTest {

    private ResponseHeaderFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new ResponseHeaderFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("injects gateway timestamp and security response headers")
    void doFilterInternal_InjectsHeaders() throws ServletException, IOException {
        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader(ResponseHeaderFilter.GATEWAY_TIMESTAMP_HEADER)).isNotNull();
        assertThat(response.getHeader(ResponseHeaderFilter.SERVER_HEADER)).isEqualTo("MovieBooking-Gateway/1.0");
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(response.getHeader("X-XSS-Protection")).isEqualTo("1; mode=block");

        verify(filterChain).doFilter(request, response);
    }
}
