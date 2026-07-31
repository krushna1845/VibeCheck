package com.krushna.moviebooking.booking.validator;

import com.krushna.moviebooking.booking.client.ShowClient.ShowSeatDto;
import com.krushna.moviebooking.booking.exception.SeatInactiveException;
import com.krushna.moviebooking.booking.exception.SeatNotAvailableException;
import com.krushna.moviebooking.booking.exception.SeatNotFoundException;
import com.krushna.moviebooking.booking.validator.impl.SeatValidatorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class SeatValidatorTest {

    private SeatValidatorImpl seatValidator;
    private UUID showId;
    private UUID seatId1;
    private UUID seatId2;

    @BeforeEach
    void setUp() {
        seatValidator = new SeatValidatorImpl();
        showId = UUID.randomUUID();
        seatId1 = UUID.randomUUID();
        seatId2 = UUID.randomUUID();
    }

    @Test
    @DisplayName("validateSeats should pass when all seats exist, belong to show, active, and available")
    void validateSeats_AllValid() {
        List<ShowSeatDto> fetched = List.of(
                new ShowSeatDto(seatId1, showId, seatId1, "A1", new BigDecimal("250.00"), "AVAILABLE"),
                new ShowSeatDto(seatId2, showId, seatId2, "A2", new BigDecimal("250.00"), "AVAILABLE")
        );

        assertThatCode(() -> seatValidator.validateSeats(showId, List.of(seatId1, seatId2), fetched))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateSeatsExist should throw SeatNotFoundException when requested seats missing")
    void validateSeatsExist_MissingSeat() {
        List<ShowSeatDto> fetched = List.of(
                new ShowSeatDto(seatId1, showId, seatId1, "A1", new BigDecimal("250.00"), "AVAILABLE")
        );

        assertThatThrownBy(() -> seatValidator.validateSeatsExist(showId, List.of(seatId1, seatId2), fetched))
                .isInstanceOf(SeatNotFoundException.class)
                .hasMessageContaining("do not exist");
    }

    @Test
    @DisplayName("validateSeatsBelongToShow should throw SeatNotFoundException when seat belongs to another show")
    void validateSeatsBelongToShow_MismatchShow() {
        UUID strangerShowId = UUID.randomUUID();
        List<ShowSeatDto> fetched = List.of(
                new ShowSeatDto(seatId1, strangerShowId, seatId1, "A1", new BigDecimal("250.00"), "AVAILABLE")
        );

        assertThatThrownBy(() -> seatValidator.validateSeatsBelongToShow(showId, fetched))
                .isInstanceOf(SeatNotFoundException.class);
    }

    @Test
    @DisplayName("validateSeatsActive should throw SeatInactiveException when seat status is BLOCKED")
    void validateSeatsActive_BlockedSeat() {
        List<ShowSeatDto> fetched = List.of(
                new ShowSeatDto(seatId1, showId, seatId1, "A1", new BigDecimal("250.00"), "BLOCKED")
        );

        assertThatThrownBy(() -> seatValidator.validateSeatsActive(showId, fetched))
                .isInstanceOf(SeatInactiveException.class)
                .hasMessageContaining("inactive or blocked");
    }

    @Test
    @DisplayName("validateSeatsAvailable should throw SeatNotAvailableException when seat status is LOCKED or BOOKED")
    void validateSeatsAvailable_LockedSeat() {
        List<ShowSeatDto> fetched = List.of(
                new ShowSeatDto(seatId1, showId, seatId1, "A1", new BigDecimal("250.00"), "LOCKED")
        );

        assertThatThrownBy(() -> seatValidator.validateSeatsAvailable(showId, fetched))
                .isInstanceOf(SeatNotAvailableException.class)
                .hasMessageContaining("not available");
    }
}
