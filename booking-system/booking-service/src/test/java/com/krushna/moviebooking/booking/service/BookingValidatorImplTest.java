package com.krushna.moviebooking.booking.service;

import com.krushna.moviebooking.booking.client.ShowClient;
import com.krushna.moviebooking.booking.dto.BookingRequest;
import com.krushna.moviebooking.booking.exception.InvalidBookingRequestException;
import com.krushna.moviebooking.booking.exception.SeatNotAvailableException;
import com.krushna.moviebooking.booking.service.impl.BookingValidatorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingValidatorImplTest {

    @Mock
    private ShowClient showClient;

    @InjectMocks
    private BookingValidatorImpl bookingValidator;

    private UUID userId;
    private UUID showId;
    private UUID showSeatId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        showId = UUID.randomUUID();
        showSeatId = UUID.randomUUID();
    }

    @Test
    @DisplayName("validateBookingRequest passes when all domain rules are satisfied")
    void validateBookingRequest_Success() {
        BookingRequest request = BookingRequest.builder()
                .userId(userId)
                .showId(showId)
                .showSeatIds(List.of(showSeatId))
                .build();

        ShowClient.ShowSeatDto seatDto = new ShowClient.ShowSeatDto(
                showSeatId, showId, showSeatId, "A1", new BigDecimal("200.00"), "AVAILABLE");

        when(showClient.existsShow(showId)).thenReturn(true);
        when(showClient.getShowSeatsByIds(showId, List.of(showSeatId))).thenReturn(List.of(seatDto));

        assertThatNoException().isThrownBy(() -> bookingValidator.validateBookingRequest(request));
    }

    @Test
    @DisplayName("validateBookingRequest throws InvalidBookingRequestException when userId is null")
    void validateBookingRequest_NullUser() {
        BookingRequest request = BookingRequest.builder()
                .userId(null)
                .showId(showId)
                .showSeatIds(List.of(showSeatId))
                .build();

        assertThatThrownBy(() -> bookingValidator.validateBookingRequest(request))
                .isInstanceOf(InvalidBookingRequestException.class)
                .hasMessageContaining("User reference ID must not be null");
    }

    @Test
    @DisplayName("validateBookingRequest throws InvalidBookingRequestException when show does not exist")
    void validateBookingRequest_ShowNotFound() {
        BookingRequest request = BookingRequest.builder()
                .userId(userId)
                .showId(showId)
                .showSeatIds(List.of(showSeatId))
                .build();

        when(showClient.existsShow(showId)).thenReturn(false);

        assertThatThrownBy(() -> bookingValidator.validateBookingRequest(request))
                .isInstanceOf(InvalidBookingRequestException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    @DisplayName("validateBookingRequest throws SeatNotAvailableException when selected seat is not AVAILABLE")
    void validateBookingRequest_SeatUnavailable() {
        BookingRequest request = BookingRequest.builder()
                .userId(userId)
                .showId(showId)
                .showSeatIds(List.of(showSeatId))
                .build();

        ShowClient.ShowSeatDto seatDto = new ShowClient.ShowSeatDto(
                showSeatId, showId, showSeatId, "A1", new BigDecimal("200.00"), "BOOKED");

        when(showClient.existsShow(showId)).thenReturn(true);
        when(showClient.getShowSeatsByIds(showId, List.of(showSeatId))).thenReturn(List.of(seatDto));

        assertThatThrownBy(() -> bookingValidator.validateBookingRequest(request))
                .isInstanceOf(SeatNotAvailableException.class);
    }
}
