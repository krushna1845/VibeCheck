package com.krushna.moviebooking.booking.controller;

import com.krushna.moviebooking.booking.dto.*;
import com.krushna.moviebooking.booking.exception.BookingNotFoundException;
import com.krushna.moviebooking.booking.exception.GlobalExceptionHandler;
import com.krushna.moviebooking.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

    private UUID userId;
    private UUID showId;
    private UUID showSeatId;
    private UUID bookingId;
    private String bookingRef;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        userId = UUID.randomUUID();
        showId = UUID.randomUUID();
        showSeatId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        bookingRef = "BK9999999999";
    }

    @Test
    @DisplayName("POST /api/v1/bookings creates booking and returns HTTP 201")
    void createBooking_Returns201() throws Exception {
        String jsonPayload = String.format("""
                {
                    "userId": "%s",
                    "showId": "%s",
                    "showSeatIds": ["%s"]
                }
                """, userId, showId, showSeatId);

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
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingReference").value(bookingRef))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("PUT /api/v1/bookings/{id} updates booking and returns HTTP 200")
    void updateBooking_Returns200() throws Exception {
        String jsonPayload = """
                {
                    "status": "CONFIRMED"
                }
                """;

        BookingResponse response = BookingResponse.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .status("CONFIRMED")
                .build();

        when(bookingService.updateBooking(eq(bookingId), any(BookingUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/bookings/{id}", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("POST /api/v1/bookings/confirm confirms booking and returns HTTP 200")
    void confirmBooking_Returns200() throws Exception {
        String jsonPayload = String.format("""
                {
                    "bookingReference": "%s",
                    "status": "SUCCESS",
                    "paymentId": "PAY-100"
                }
                """, bookingRef);

        BookingResponse response = BookingResponse.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .status("CONFIRMED")
                .build();

        when(bookingService.confirmBooking(eq(bookingRef), eq("PAY-100"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/bookings/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("DELETE /api/v1/bookings/{id} soft deletes booking and returns HTTP 244 No Content")
    void deleteBooking_Returns204() throws Exception {
        doNothing().when(bookingService).deleteBooking(bookingId);

        mockMvc.perform(delete("/api/v1/bookings/{id}", bookingId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/v1/bookings/{reference} returns HTTP 404 when booking not found")
    void getBookingByReference_Returns404() throws Exception {
        when(bookingService.getBookingByReference("NON_EXISTENT"))
                .thenThrow(new BookingNotFoundException("NON_EXISTENT"));

        mockMvc.perform(get("/api/v1/bookings/{reference}", "NON_EXISTENT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Booking Not Found"));
    }
}
