package com.krushna.moviebooking.booking.validator;

import com.krushna.moviebooking.booking.dto.BookingRequest;
import com.krushna.moviebooking.booking.entity.Booking;
import com.krushna.moviebooking.booking.exception.InvalidBookingOwnershipException;
import com.krushna.moviebooking.booking.exception.InvalidBookingRequestException;
import com.krushna.moviebooking.booking.exception.InvalidBookingStateException;
import com.krushna.moviebooking.booking.validator.impl.BookingValidatorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class BookingValidatorTest {

    private BookingValidatorImpl bookingValidator;

    @BeforeEach
    void setUp() {
        bookingValidator = new BookingValidatorImpl();
    }

    // -------------------------------------------------------------------------
    // validateBookingRequest tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("validateBookingRequest should pass for valid payload")
    void validateBookingRequest_Valid() {
        BookingRequest request = BookingRequest.builder()
                .userId(UUID.randomUUID())
                .showId(UUID.randomUUID())
                .showSeatIds(List.of(UUID.randomUUID(), UUID.randomUUID()))
                .build();

        assertThatCode(() -> bookingValidator.validateBookingRequest(request))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateBookingRequest should throw when request is null")
    void validateBookingRequest_NullRequest() {
        assertThatThrownBy(() -> bookingValidator.validateBookingRequest(null))
                .isInstanceOf(InvalidBookingRequestException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    @DisplayName("validateBookingRequest should throw when userId is null")
    void validateBookingRequest_NullUserId() {
        BookingRequest request = BookingRequest.builder()
                .userId(null)
                .showId(UUID.randomUUID())
                .showSeatIds(List.of(UUID.randomUUID()))
                .build();

        assertThatThrownBy(() -> bookingValidator.validateBookingRequest(request))
                .isInstanceOf(InvalidBookingRequestException.class)
                .hasMessageContaining("User reference ID must not be null");
    }

    @Test
    @DisplayName("validateBookingRequest should throw when showId is null")
    void validateBookingRequest_NullShowId() {
        BookingRequest request = BookingRequest.builder()
                .userId(UUID.randomUUID())
                .showId(null)
                .showSeatIds(List.of(UUID.randomUUID()))
                .build();

        assertThatThrownBy(() -> bookingValidator.validateBookingRequest(request))
                .isInstanceOf(InvalidBookingRequestException.class)
                .hasMessageContaining("Show reference ID must not be null");
    }

    @Test
    @DisplayName("validateBookingRequest should throw when seat list is empty")
    void validateBookingRequest_EmptySeats() {
        BookingRequest request = BookingRequest.builder()
                .userId(UUID.randomUUID())
                .showId(UUID.randomUUID())
                .showSeatIds(List.of())
                .build();

        assertThatThrownBy(() -> bookingValidator.validateBookingRequest(request))
                .isInstanceOf(InvalidBookingRequestException.class)
                .hasMessageContaining("At least one seat must be selected");
    }

    @Test
    @DisplayName("validateBookingRequest should throw when exceeding max seat limit")
    void validateBookingRequest_ExceedMaxSeats() {
        List<UUID> tooManySeats = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            tooManySeats.add(UUID.randomUUID());
        }

        BookingRequest request = BookingRequest.builder()
                .userId(UUID.randomUUID())
                .showId(UUID.randomUUID())
                .showSeatIds(tooManySeats)
                .build();

        assertThatThrownBy(() -> bookingValidator.validateBookingRequest(request))
                .isInstanceOf(InvalidBookingRequestException.class)
                .hasMessageContaining("Cannot book more than 10 seats");
    }

    // -------------------------------------------------------------------------
    // validateBookingOwnership tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("validateBookingOwnership should pass when userId matches booking userId")
    void validateBookingOwnership_Valid() {
        UUID userId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .build();

        assertThatCode(() -> bookingValidator.validateBookingOwnership(booking, userId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateBookingOwnership should throw when userId does not match booking userId")
    void validateBookingOwnership_Mismatch() {
        UUID ownerId = UUID.randomUUID();
        UUID attackerId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .userId(ownerId)
                .build();

        assertThatThrownBy(() -> bookingValidator.validateBookingOwnership(booking, attackerId))
                .isInstanceOf(InvalidBookingOwnershipException.class)
                .hasMessageContaining("is not the owner of booking");
    }

    // -------------------------------------------------------------------------
    // validateBookingState tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("validateBookingState should allow legal transition CREATED -> PENDING")
    void validateBookingState_LegalTransition() {
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .status("CREATED")
                .build();

        assertThatCode(() -> bookingValidator.validateBookingState(booking, "PENDING"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateBookingState should throw on illegal transition COMPLETED -> CANCELLED")
    void validateBookingState_IllegalTransition() {
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .status("COMPLETED")
                .build();

        assertThatThrownBy(() -> bookingValidator.validateBookingState(booking, "CANCELLED"))
                .isInstanceOf(InvalidBookingStateException.class)
                .hasMessageContaining("Illegal booking state transition");
    }

    @Test
    @DisplayName("validateBookingState should be idempotent when status is identical")
    void validateBookingState_IdempotentSameState() {
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .status("CONFIRMED")
                .build();

        assertThatCode(() -> bookingValidator.validateBookingState(booking, "CONFIRMED"))
                .doesNotThrowAnyException();
    }
}
