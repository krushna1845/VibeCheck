package com.krushna.moviebooking.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krushna.moviebooking.booking.dto.BookingResponse;
import com.krushna.moviebooking.booking.dto.BookingSearchCriteria;
import com.krushna.moviebooking.booking.dto.BookingSummary;
import com.krushna.moviebooking.booking.dto.BookingUpdateRequest;
import com.krushna.moviebooking.booking.exception.BookingExceptionHandler;
import com.krushna.moviebooking.booking.exception.BookingNotFoundException;
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
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookingAdminControllerTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingAdminController bookingAdminController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID bookingId;
    private UUID userId;
    private UUID showId;
    private String bookingRef;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(bookingAdminController)
                .setControllerAdvice(new BookingExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        bookingId = UUID.randomUUID();
        userId = UUID.randomUUID();
        showId = UUID.randomUUID();
        bookingRef = "BK8888888888";
    }

    @Test
    @DisplayName("GET /api/v1/admin/bookings performs admin search with criteria and returns HTTP 200 OK")
    void searchBookings_Returns200() throws Exception {
        BookingSummary summary = BookingSummary.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .userId(userId)
                .showId(showId)
                .totalAmount(new BigDecimal("350.00"))
                .status("CONFIRMED")
                .seatCount(3)
                .build();

        PageImpl<BookingSummary> page = new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1);
        when(bookingService.adminSearch(any(BookingSearchCriteria.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/bookings")
                        .param("status", "CONFIRMED")
                        .param("userId", userId.toString())
                        .param("showId", showId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Admin search completed successfully"))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].bookingReference").value(bookingRef))
                .andExpect(jsonPath("$.data.content[0].status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/bookings/{id} retrieves detailed booking for admin and returns HTTP 200 OK")
    void getBookingById_Returns200() throws Exception {
        BookingResponse response = BookingResponse.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .userId(userId)
                .showId(showId)
                .status("CONFIRMED")
                .totalAmount(new BigDecimal("350.00"))
                .build();

        when(bookingService.getBookingById(bookingId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/bookings/{id}", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(bookingId.toString()))
                .andExpect(jsonPath("$.data.bookingReference").value(bookingRef));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/bookings/{id}/status force updates booking status and returns HTTP 200 OK")
    void forceUpdateStatus_Returns200() throws Exception {
        BookingUpdateRequest request = BookingUpdateRequest.builder()
                .status("EXPIRED")
                .build();

        BookingResponse response = BookingResponse.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .status("EXPIRED")
                .build();

        when(bookingService.updateBooking(eq(bookingId), any(BookingUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/bookings/{id}/status", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Booking status updated by administrator"))
                .andExpect(jsonPath("$.data.status").value("EXPIRED"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/bookings/{id} returns HTTP 404 ProblemDetail when booking not found")
    void getBookingById_NotFound_Returns404() throws Exception {
        when(bookingService.getBookingById(bookingId)).thenThrow(new BookingNotFoundException(bookingId));

        mockMvc.perform(get("/api/v1/admin/bookings/{id}", bookingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Booking Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }
}
