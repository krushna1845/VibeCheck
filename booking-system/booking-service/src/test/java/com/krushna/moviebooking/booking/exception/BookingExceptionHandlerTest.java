package com.krushna.moviebooking.booking.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BookingExceptionHandlerTest {

    private BookingExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new BookingExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/bookings/test");
    }

    @Test
    @DisplayName("handleBookingNotFound returns 404 ProblemDetail with booking-not-found type")
    void handleBookingNotFound() {
        UUID id = UUID.randomUUID();
        BookingNotFoundException ex = new BookingNotFoundException(id);

        ProblemDetail problem = handler.handleBookingNotFound(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Booking Not Found");
        assertThat(problem.getDetail()).contains(id.toString());
        assertThat(problem.getType().toString()).endsWith("booking-not-found");
        assertThat(problem.getInstance().toString()).isEqualTo("/api/v1/bookings/test");
    }

    @Test
    @DisplayName("handleSeatConflict returns 409 ProblemDetail")
    void handleSeatConflict() {
        SeatAlreadyLockedException ex = new SeatAlreadyLockedException(UUID.randomUUID(), List.of(UUID.randomUUID()));

        ProblemDetail problem = handler.handleSeatConflict(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Seat Conflict");
        assertThat(problem.getDetail()).contains("locked for showId");
    }

    @Test
    @DisplayName("handleExpired returns 410 ProblemDetail")
    void handleExpired() {
        BookingExpiredException ex = new BookingExpiredException("BK12345");

        ProblemDetail problem = handler.handleExpired(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.GONE.value());
        assertThat(problem.getTitle()).isEqualTo("Booking Reservation Expired");
    }

    @Test
    @DisplayName("handlePaymentFailed returns 402 ProblemDetail")
    void handlePaymentFailed() {
        PaymentFailedException ex = new PaymentFailedException("Card declined");

        ProblemDetail problem = handler.handlePaymentFailed(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.PAYMENT_REQUIRED.value());
        assertThat(problem.getTitle()).isEqualTo("Payment Failed");
    }

    @Test
    @DisplayName("handleGenericException returns 500 ProblemDetail")
    void handleGenericException() {
        RuntimeException ex = new RuntimeException("Unexpected error");

        ProblemDetail problem = handler.handleGenericException(ex, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
    }
}
