package com.krushna.moviebooking.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krushna.moviebooking.booking.client.ShowClient;
import com.krushna.moviebooking.booking.dto.BookingRequest;
import com.krushna.moviebooking.booking.dto.BookingResponse;
import com.krushna.moviebooking.booking.dto.SeatLockRequest;
import com.krushna.moviebooking.booking.dto.SeatLockResponse;
import com.krushna.moviebooking.booking.entity.Booking;
import com.krushna.moviebooking.booking.event.BookingEventPublisher;
import com.krushna.moviebooking.booking.event.SeatAvailabilityPublisher;
import com.krushna.moviebooking.booking.idempotency.BookingIdempotencyService;
import com.krushna.moviebooking.booking.mapper.BookingMapper;
import com.krushna.moviebooking.booking.repository.BookingRepository;
import com.krushna.moviebooking.booking.service.BookingService;
import com.krushna.moviebooking.booking.service.SeatLockService;
import com.krushna.moviebooking.booking.service.impl.BookingServiceImpl;
import com.krushna.moviebooking.booking.validator.BookingValidationFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Booking HTTP Idempotency Hardening Tests")
class BookingHttpIdempotencyTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private BookingValidationFacade bookingValidationFacade;
    @Mock private SeatLockService seatLockService;
    @Mock private ShowClient showClient;
    @Mock private BookingEventPublisher bookingEventPublisher;
    @Mock private BookingMapper bookingMapper;
    @Mock private SeatAvailabilityPublisher seatAvailabilityPublisher;
    @Mock private BookingIdempotencyService bookingIdempotencyService;

    private BookingServiceImpl bookingService;
    private BookingController bookingController;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    private UUID userId;
    private UUID showId;
    private UUID seatId;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl(
                bookingRepository,
                bookingValidationFacade,
                seatLockService,
                showClient,
                null,
                bookingEventPublisher,
                bookingMapper,
                seatAvailabilityPublisher,
                bookingIdempotencyService
        );

        bookingController = new BookingController(bookingService);
        mockMvc = MockMvcBuilders.standaloneSetup(bookingController).build();

        userId = UUID.randomUUID();
        showId = UUID.randomUUID();
        seatId = UUID.randomUUID();
    }

    @Test
    @DisplayName("TEST: Duplicate booking with same idempotency key -> returns same booking without duplicate seat locking")
    void duplicateBooking_SameIdempotencyKey_ReturnsSameBooking() {
        String idempotencyKey = "idem-booking-12345";
        BookingRequest request = BookingRequest.builder()
                .userId(userId)
                .showId(showId)
                .showSeatIds(List.of(seatId))
                .idempotencyKey(idempotencyKey)
                .build();

        UUID bookingId = UUID.randomUUID();
        BookingResponse originalResponse = BookingResponse.builder()
                .id(bookingId)
                .bookingReference("BK1234567890")
                .userId(userId)
                .showId(showId)
                .totalAmount(new BigDecimal("250.00"))
                .status("PENDING")
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        // Simulate cache miss on first call
        when(bookingIdempotencyService.findCachedResponse(idempotencyKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(originalResponse));

        when(seatLockService.lockSeats(any(SeatLockRequest.class)))
                .thenReturn(SeatLockResponse.builder().success(true).build());

        when(showClient.getShowSeatsByIds(eq(showId), anyList()))
                .thenReturn(List.of(new ShowClient.ShowSeatDto(seatId, showId, UUID.randomUUID(), "A1", new BigDecimal("200.00"), "AVAILABLE")));

        Booking mockBooking = Booking.builder()
                .id(bookingId)
                .bookingReference("BK1234567890")
                .userId(userId)
                .showId(showId)
                .totalAmount(new BigDecimal("250.00"))
                .status("PENDING")
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);
        when(bookingMapper.toResponse(any())).thenReturn(originalResponse);

        // 1. First invocation: creates booking and caches it
        BookingResponse firstCall = bookingService.createBooking(request);
        assertThat(firstCall.bookingReference()).isEqualTo("BK1234567890");
        verify(seatLockService, times(1)).lockSeats(any());
        verify(bookingIdempotencyService).cacheResponse(eq(idempotencyKey), eq(originalResponse));

        // 2. Second invocation with same idempotency key: returns cached booking immediately
        BookingResponse secondCall = bookingService.createBooking(request);
        assertThat(secondCall.bookingReference()).isEqualTo("BK1234567890");
        assertThat(secondCall.id()).isEqualTo(bookingId);

        // Lock seats must NOT be called a second time (which would cause a seat lock collision!)
        verify(seatLockService, times(1)).lockSeats(any());
    }

    @Test
    @DisplayName("TEST: Controller propagates Idempotency-Key header to BookingRequest")
    void controllerPropagatesIdempotencyKeyHeader() throws Exception {
        String idempotencyKey = "header-key-999";
        BookingRequest requestPayload = BookingRequest.builder()
                .userId(userId)
                .showId(showId)
                .showSeatIds(List.of(seatId))
                .build();

        UUID bookingId = UUID.randomUUID();
        BookingResponse response = BookingResponse.builder()
                .id(bookingId)
                .bookingReference("BK9999999999")
                .userId(userId)
                .showId(showId)
                .totalAmount(new BigDecimal("300.00"))
                .status("PENDING")
                .build();

        when(bookingIdempotencyService.findCachedResponse(idempotencyKey)).thenReturn(Optional.of(response));

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bookingReference").value("BK9999999999"))
                .andExpect(jsonPath("$.data.totalAmount").value(300.00));

        // Verifies idempotency check was invoked with the header key
        verify(bookingIdempotencyService).findCachedResponse(idempotencyKey);
    }
}
