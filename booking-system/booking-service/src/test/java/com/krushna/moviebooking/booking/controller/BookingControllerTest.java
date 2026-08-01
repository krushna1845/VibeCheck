package com.krushna.moviebooking.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krushna.moviebooking.booking.dto.*;
import com.krushna.moviebooking.booking.exception.BookingExceptionHandler;
import com.krushna.moviebooking.booking.exception.BookingNotFoundException;
import com.krushna.moviebooking.booking.exception.SeatUnavailableException;
import com.krushna.moviebooking.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID userId;
    private UUID showId;
    private UUID showSeatId;
    private UUID bookingId;
    private String bookingRef;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
                .setControllerAdvice(new BookingExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        userId = UUID.randomUUID();
        showId = UUID.randomUUID();
        showSeatId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        bookingRef = "BK9999999999";
    }

    @Test
    @DisplayName("POST /api/v1/bookings creates booking and returns HTTP 201 Created")
    void createBooking_Returns201() throws Exception {
        BookingRequest request = BookingRequest.builder()
                .userId(userId)
                .showId(showId)
                .showSeatIds(List.of(showSeatId))
                .build();

        BookingResponse response = BookingResponse.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .userId(userId)
                .showId(showId)
                .status("PENDING")
                .totalAmount(new BigDecimal("266.00"))
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        when(bookingService.createBooking(any(BookingRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Booking created successfully"))
                .andExpect(jsonPath("$.data.bookingReference").value(bookingRef))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/bookings returns HTTP 409 Conflict when seats locked/unavailable")
    void createBooking_SeatUnavailable_Returns409() throws Exception {
        BookingRequest request = BookingRequest.builder()
                .userId(userId)
                .showId(showId)
                .showSeatIds(List.of(showSeatId))
                .build();

        when(bookingService.createBooking(any(BookingRequest.class)))
                .thenThrow(new SeatUnavailableException(showId, List.of(showSeatId)));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Seat Conflict"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("PUT /api/v1/bookings/{id} updates booking and returns HTTP 200 OK")
    void updateBooking_Returns200() throws Exception {
        BookingUpdateRequest request = BookingUpdateRequest.builder()
                .status("CONFIRMED")
                .build();

        BookingResponse response = BookingResponse.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .status("CONFIRMED")
                .build();

        when(bookingService.updateBooking(eq(bookingId), any(BookingUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/bookings/{id}", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("POST /api/v1/bookings/confirm confirms booking and returns HTTP 200 OK")
    void confirmBooking_Returns200() throws Exception {
        PaymentCallbackRequest callback = PaymentCallbackRequest.builder()
                .bookingReference(bookingRef)
                .paymentId("PAY-100")
                .status("SUCCESS")
                .build();

        BookingResponse response = BookingResponse.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .status("CONFIRMED")
                .build();

        when(bookingService.confirmBooking(eq(bookingRef), eq("PAY-100"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/bookings/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callback)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("POST /api/v1/bookings/{reference}/cancel cancels booking and returns HTTP 200 OK")
    void cancelBooking_Returns200() throws Exception {
        BookingResponse response = BookingResponse.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .status("CANCELLED")
                .build();

        when(bookingService.cancelBooking(eq(bookingRef), any(String.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/bookings/{reference}/cancel", bookingRef)
                        .param("reason", "Changed mind"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("DELETE /api/v1/bookings/{id} soft deletes booking and returns HTTP 204 No Content")
    void deleteBooking_Returns204() throws Exception {
        doNothing().when(bookingService).deleteBooking(bookingId);

        mockMvc.perform(delete("/api/v1/bookings/{id}", bookingId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/v1/bookings/{reference} returns HTTP 404 ProblemDetail when reference not found")
    void getBookingByReference_Returns404() throws Exception {
        when(bookingService.getBookingByReference("NON_EXISTENT"))
                .thenThrow(new BookingNotFoundException("NON_EXISTENT"));

        mockMvc.perform(get("/api/v1/bookings/{reference}", "NON_EXISTENT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Booking Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/v1/bookings/user/{userId}/history returns paginated booking history")
    void getUserBookingHistory_Returns200() throws Exception {
        BookingSummary summary = BookingSummary.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .userId(userId)
                .showId(showId)
                .totalAmount(new BigDecimal("266.00"))
                .status("CONFIRMED")
                .seatCount(2)
                .build();

        PageImpl<BookingSummary> page = new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1);
        when(bookingService.getUserBookings(eq(userId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/bookings/user/{userId}/history", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].bookingReference").value(bookingRef))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }
}
