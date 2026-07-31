package com.krushna.moviebooking.booking.scheduler;

import com.krushna.moviebooking.booking.entity.Booking;
import com.krushna.moviebooking.booking.entity.BookingSeat;
import com.krushna.moviebooking.booking.event.BookingEventPublisher;
import com.krushna.moviebooking.booking.event.BookingExpiredEvent;
import com.krushna.moviebooking.booking.repository.BookingRepository;
import com.krushna.moviebooking.booking.service.SeatLockService;
import com.krushna.moviebooking.booking.statemachine.BookingStateMachine;
import com.krushna.moviebooking.booking.statemachine.BookingStatus;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpiredBookingProcessorTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SeatLockService seatLockService;

    @Mock
    private BookingEventPublisher bookingEventPublisher;

    @Mock
    private BookingStateMachine bookingStateMachine;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private ExpiredBookingProcessor expiredBookingProcessor;

    private UUID bookingId;
    private UUID showId;
    private UUID userId;
    private UUID seatId1;
    private UUID seatId2;
    private Booking sampleBooking;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        showId = UUID.randomUUID();
        userId = UUID.randomUUID();
        seatId1 = UUID.randomUUID();
        seatId2 = UUID.randomUUID();

        BookingSeat seat1 = BookingSeat.builder().id(UUID.randomUUID()).showSeatId(seatId1).seatNumber("A1").build();
        BookingSeat seat2 = BookingSeat.builder().id(UUID.randomUUID()).showSeatId(seatId2).seatNumber("A2").build();

        sampleBooking = Booking.builder()
                .id(bookingId)
                .bookingReference("BKG123456789")
                .userId(userId)
                .showId(showId)
                .status("PENDING")
                .expiresAt(Instant.now().minusSeconds(60))
                .bookingSeats(List.of(seat1, seat2))
                .build();
    }

    @Test
    @DisplayName("processExpiredBooking successfully releases Redis locks, transitions state, saves entity, publishes event, and records metrics")
    void processExpiredBooking_Success() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(sampleBooking));
        when(bookingStateMachine.currentStatus(sampleBooking)).thenReturn(BookingStatus.CREATED);

        expiredBookingProcessor.processExpiredBooking(bookingId);

        verify(seatLockService).releaseLocks(showId, List.of(seatId1, seatId2));
        verify(bookingStateMachine).transition(sampleBooking, BookingStatus.EXPIRED);
        verify(bookingRepository).save(sampleBooking);

        ArgumentCaptor<BookingExpiredEvent> eventCaptor = ArgumentCaptor.forClass(BookingExpiredEvent.class);
        verify(bookingEventPublisher).publishBookingExpired(eventCaptor.capture());

        BookingExpiredEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.bookingId()).isEqualTo(bookingId);
        assertThat(publishedEvent.bookingReference()).isEqualTo("BKG123456789");
        assertThat(publishedEvent.showSeatIds()).containsExactlyInAnyOrder(seatId1, seatId2);

        assertThat(meterRegistry.find("booking.expiration.success").counter()).isNotNull();
        assertThat(meterRegistry.find("booking.expiration.success").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("processExpiredBooking skips processing if booking is already in terminal state")
    void processExpiredBooking_AlreadyTerminal() {
        sampleBooking.setStatus("EXPIRED");
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(sampleBooking));
        when(bookingStateMachine.currentStatus(sampleBooking)).thenReturn(BookingStatus.EXPIRED);

        expiredBookingProcessor.processExpiredBooking(bookingId);

        verify(seatLockService, never()).releaseLocks(any(), any());
        verify(bookingStateMachine, never()).transition(any(Booking.class), any(BookingStatus.class));
        verify(bookingRepository, never()).save(any());
        verify(bookingEventPublisher, never()).publishBookingExpired(any());
    }

    @Test
    @DisplayName("processExpiredBooking skips processing if booking is not found or soft-deleted")
    void processExpiredBooking_NotFoundOrDeleted() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        expiredBookingProcessor.processExpiredBooking(bookingId);

        verify(seatLockService, never()).releaseLocks(any(), any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("recoverExpiredBooking increments failure metric when retries are exhausted")
    void recoverExpiredBooking_Success() {
        Exception exception = new RuntimeException("Database timeout");

        expiredBookingProcessor.recoverExpiredBooking(exception, bookingId);

        assertThat(meterRegistry.find("booking.expiration.failure").counter()).isNotNull();
        assertThat(meterRegistry.find("booking.expiration.failure").counter().count()).isEqualTo(1.0);
    }
}
