package com.krushna.moviebooking.booking.validator;

import com.krushna.moviebooking.booking.client.ShowClient;
import com.krushna.moviebooking.booking.client.ShowClient.ShowDto;
import com.krushna.moviebooking.booking.client.ShowClient.ShowSeatDto;
import com.krushna.moviebooking.booking.client.UserClient;
import com.krushna.moviebooking.booking.client.UserClient.UserDto;
import com.krushna.moviebooking.booking.dto.BookingRequest;
import com.krushna.moviebooking.booking.entity.Booking;
import com.krushna.moviebooking.booking.exception.InvalidBookingRequestException;
import com.krushna.moviebooking.booking.validator.impl.BookingValidationFacadeImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingValidationFacadeTest {

    @Mock
    private BookingValidator bookingValidator;
    @Mock
    private SeatValidator seatValidator;
    @Mock
    private ShowValidator showValidator;
    @Mock
    private UserValidator userValidator;
    @Mock
    private ShowClient showClient;
    @Mock
    private UserClient userClient;

    private BookingValidationFacadeImpl facade;

    private UUID userId;
    private UUID showId;
    private UUID seatId;

    @BeforeEach
    void setUp() {
        facade = new BookingValidationFacadeImpl(
                bookingValidator, seatValidator, showValidator, userValidator, showClient, userClient
        );
        userId = UUID.randomUUID();
        showId = UUID.randomUUID();
        seatId = UUID.randomUUID();
    }

    @Test
    @DisplayName("validateBookingCreation should orchestrate all 4 validators sequentially")
    void validateBookingCreation_OrchestratesSuccessfully() {
        BookingRequest request = BookingRequest.builder()
                .userId(userId)
                .showId(showId)
                .showSeatIds(List.of(seatId))
                .build();

        UserDto userDto = new UserDto(userId, "User", "u@e.com", "ACTIVE", List.of("ROLE_CUSTOMER"));
        ShowDto showDto = new ShowDto(showId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "SCHEDULED", Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200));
        ShowSeatDto seatDto = new ShowSeatDto(seatId, showId, seatId, "A1", new BigDecimal("250.00"), "AVAILABLE");

        when(userClient.getUserById(userId)).thenReturn(Optional.of(userDto));
        when(showClient.getShowById(showId)).thenReturn(Optional.of(showDto));
        when(showClient.getShowSeatsByIds(showId, List.of(seatId))).thenReturn(List.of(seatDto));

        assertThatCode(() -> facade.validateBookingCreation(request)).doesNotThrowAnyException();

        verify(bookingValidator).validateBookingRequest(request);
        verify(userValidator).validateUser(Optional.of(userDto), userId);
        verify(showValidator).validateShow(Optional.of(showDto), showId);
        verify(seatValidator).validateSeats(showId, List.of(seatId), List.of(seatDto));
    }

    @Test
    @DisplayName("validateBookingConfirmation should throw when paymentId is blank")
    void validateBookingConfirmation_BlankPaymentId() {
        Booking booking = Booking.builder().id(UUID.randomUUID()).status("PENDING").build();

        assertThatThrownBy(() -> facade.validateBookingConfirmation(booking, ""))
                .isInstanceOf(InvalidBookingRequestException.class)
                .hasMessageContaining("Payment reference ID must not be null or blank");
    }

    @Test
    @DisplayName("validateBookingCancellation should delegate ownership and state transition validation")
    void validateBookingCancellation_Delegates() {
        Booking booking = Booking.builder().id(UUID.randomUUID()).userId(userId).status("PENDING").build();

        assertThatCode(() -> facade.validateBookingCancellation(booking, userId)).doesNotThrowAnyException();

        verify(bookingValidator).validateBookingOwnership(booking, userId);
        verify(bookingValidator).validateBookingState(booking, "CANCELLED");
    }
}
