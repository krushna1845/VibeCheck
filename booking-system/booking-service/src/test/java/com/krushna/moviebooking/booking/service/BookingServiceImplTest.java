package com.krushna.moviebooking.booking.service;

import com.krushna.moviebooking.booking.client.PaymentClient;
import com.krushna.moviebooking.booking.client.ShowClient;
import com.krushna.moviebooking.booking.dto.*;
import com.krushna.moviebooking.booking.entity.Booking;
import com.krushna.moviebooking.booking.entity.BookingSeat;
import com.krushna.moviebooking.booking.event.BookingEventPublisher;
import com.krushna.moviebooking.booking.exception.*;
import com.krushna.moviebooking.booking.mapper.BookingMapper;
import com.krushna.moviebooking.booking.repository.BookingRepository;
import com.krushna.moviebooking.booking.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingValidator bookingValidator;

    @Mock
    private SeatLockService seatLockService;

    @Mock
    private ShowClient showClient;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private BookingEventPublisher bookingEventPublisher;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private UUID userId;
    private UUID showId;
    private UUID showSeatId;
    private UUID bookingId;
    private String bookingRef;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        showId = UUID.randomUUID();
        showSeatId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        bookingRef = "BK1234567890";
    }

    @Test
    @DisplayName("createBooking successfully locks seats and persists PENDING booking")
    void createBooking_Success() {
        BookingRequest request = BookingRequest.builder()
                .userId(userId)
                .showId(showId)
                .showSeatIds(List.of(showSeatId))
                .build();

        SeatLockResponse lockResponse = SeatLockResponse.builder()
                .success(true)
                .showId(showId)
                .lockedSeatIds(List.of(showSeatId))
                .build();

        ShowClient.ShowSeatDto seatDto = new ShowClient.ShowSeatDto(
                showSeatId, showId, showSeatId, "A1", new BigDecimal("200.00"), "AVAILABLE");

        Booking savedBooking = Booking.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .userId(userId)
                .showId(showId)
                .status("PENDING")
                .totalAmount(new BigDecimal("266.00"))
                .expiresAt(Instant.now().plusSeconds(300))
                .bookingSeats(new ArrayList<>())
                .build();

        BookingResponse expectedResponse = BookingResponse.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .status("PENDING")
                .build();

        when(seatLockService.lockSeats(any())).thenReturn(lockResponse);
        when(showClient.getShowSeatsByIds(eq(showId), any())).thenReturn(List.of(seatDto));
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
        when(bookingMapper.toResponse(savedBooking)).thenReturn(expectedResponse);

        BookingResponse response = bookingService.createBooking(request);

        assertThat(response).isNotNull();
        assertThat(response.bookingReference()).isEqualTo(bookingRef);
        verify(bookingValidator).validateBookingRequest(request);
        verify(seatLockService).lockSeats(any());
        verify(bookingRepository).save(any(Booking.class));
        verify(bookingEventPublisher).publishBookingCreated(any());
    }

    @Test
    @DisplayName("createBooking throws SeatUnavailableException when seat lock fails")
    void createBooking_ThrowsSeatUnavailableException() {
        BookingRequest request = BookingRequest.builder()
                .userId(userId)
                .showId(showId)
                .showSeatIds(List.of(showSeatId))
                .build();

        SeatLockResponse lockResponse = SeatLockResponse.builder()
                .success(false)
                .failedSeatIds(List.of(showSeatId))
                .build();

        when(seatLockService.lockSeats(any())).thenReturn(lockResponse);

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(SeatUnavailableException.class);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateBooking applies patch changes to booking")
    void updateBooking_Success() {
        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .status("PENDING")
                .totalAmount(new BigDecimal("200.00"))
                .build();

        BookingUpdateRequest updateRequest = BookingUpdateRequest.builder()
                .status("CONFIRMED")
                .totalAmount(new BigDecimal("250.00"))
                .build();

        BookingResponse expectedResponse = BookingResponse.builder()
                .id(bookingId)
                .status("CONFIRMED")
                .totalAmount(new BigDecimal("250.00"))
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingMapper.toResponse(booking)).thenReturn(expectedResponse);

        BookingResponse response = bookingService.updateBooking(bookingId, updateRequest);

        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(booking.getTotalAmount()).isEqualTo(new BigDecimal("250.00"));
    }

    @Test
    @DisplayName("confirmBooking successfully confirms pending booking")
    void confirmBooking_Success() {
        BookingSeat bookingSeat = BookingSeat.builder()
                .showSeatId(showSeatId)
                .seatNumber("A1")
                .price(new BigDecimal("200.00"))
                .build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .userId(userId)
                .showId(showId)
                .status("PENDING")
                .expiresAt(Instant.now().plusSeconds(300))
                .totalAmount(new BigDecimal("266.00"))
                .bookingSeats(List.of(bookingSeat))
                .build();

        BookingResponse expectedResponse = BookingResponse.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .status("CONFIRMED")
                .build();

        when(bookingRepository.findByBookingReference(bookingRef)).thenReturn(Optional.of(booking));
        when(bookingMapper.toResponse(booking)).thenReturn(expectedResponse);

        BookingResponse response = bookingService.confirmBooking(bookingRef, "PAY-123");

        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(booking.getStatus()).isEqualTo("CONFIRMED");
        verify(showClient).updateShowSeatsStatus(eq(showId), eq(List.of(showSeatId)), eq("BOOKED"));
        verify(seatLockService).releaseLocks(eq(showId), eq(List.of(showSeatId)));
        verify(bookingEventPublisher).publishBookingConfirmed(any());
    }

    @Test
    @DisplayName("confirmBooking returns existing response idempotently when already confirmed")
    void confirmBooking_IdempotentWhenAlreadyConfirmed() {
        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .status("CONFIRMED")
                .build();

        BookingResponse expectedResponse = BookingResponse.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .status("CONFIRMED")
                .build();

        when(bookingRepository.findByBookingReference(bookingRef)).thenReturn(Optional.of(booking));
        when(bookingMapper.toResponse(booking)).thenReturn(expectedResponse);

        BookingResponse response = bookingService.confirmBooking(bookingRef, "PAY-123");

        assertThat(response.status()).isEqualTo("CONFIRMED");
        verify(showClient, never()).updateShowSeatsStatus(any(), any(), any());
    }

    @Test
    @DisplayName("confirmBooking throws BookingExpiredException when booking is expired")
    void confirmBooking_ThrowsBookingExpiredException() {
        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .status("EXPIRED")
                .build();

        when(bookingRepository.findByBookingReference(bookingRef)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.confirmBooking(bookingRef, "PAY-123"))
                .isInstanceOf(BookingExpiredException.class);
    }

    @Test
    @DisplayName("cancelBooking successfully cancels booking and releases seats")
    void cancelBooking_Success() {
        BookingSeat seat = BookingSeat.builder().showSeatId(showSeatId).build();
        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .showId(showId)
                .status("PENDING")
                .bookingSeats(List.of(seat))
                .build();

        BookingResponse expectedResponse = BookingResponse.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .status("CANCELLED")
                .build();

        when(bookingRepository.findByBookingReference(bookingRef)).thenReturn(Optional.of(booking));
        when(bookingMapper.toResponse(booking)).thenReturn(expectedResponse);

        BookingResponse response = bookingService.cancelBooking(bookingRef, "User changed mind");

        assertThat(response.status()).isEqualTo("CANCELLED");
        assertThat(booking.getStatus()).isEqualTo("CANCELLED");
        verify(showClient).updateShowSeatsStatus(eq(showId), eq(List.of(showSeatId)), eq("AVAILABLE"));
        verify(seatLockService).releaseLocks(eq(showId), eq(List.of(showSeatId)));
        verify(bookingEventPublisher).publishBookingCancelled(any());
    }

    @Test
    @DisplayName("deleteBooking sets deletedAt timestamp for soft delete")
    void deleteBooking_Success() {
        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        bookingService.deleteBooking(bookingId);

        assertThat(booking.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("getBookingByReference throws BookingNotFoundException when reference does not exist")
    void getBookingByReference_ThrowsNotFound() {
        when(bookingRepository.findByBookingReference("INVALID_REF")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingByReference("INVALID_REF"))
                .isInstanceOf(BookingNotFoundException.class);
    }
}
