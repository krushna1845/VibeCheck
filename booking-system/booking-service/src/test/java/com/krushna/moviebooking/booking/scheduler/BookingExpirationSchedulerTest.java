package com.krushna.moviebooking.booking.scheduler;

import com.krushna.moviebooking.booking.entity.Booking;
import com.krushna.moviebooking.booking.repository.BookingRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingExpirationSchedulerTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ExpiredBookingProcessor expiredBookingProcessor;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private BookingExpirationScheduler bookingExpirationScheduler;

    private UUID bkgId1;
    private UUID bkgId2;

    @BeforeEach
    void setUp() {
        bkgId1 = UUID.randomUUID();
        bkgId2 = UUID.randomUUID();
    }

    @Test
    @DisplayName("sweepExpiredBookings queries expired bookings and delegates each to ExpiredBookingProcessor")
    void sweepExpiredBookings_Success() {
        Booking bkg1 = Booking.builder().id(bkgId1).bookingReference("REF1").build();
        Booking bkg2 = Booking.builder().id(bkgId2).bookingReference("REF2").build();

        when(bookingRepository.findExpiredBookingsByStatuses(any(), any(Instant.class)))
                .thenReturn(List.of(bkg1, bkg2));

        bookingExpirationScheduler.sweepExpiredBookings();

        verify(expiredBookingProcessor).processExpiredBooking(bkgId1);
        verify(expiredBookingProcessor).processExpiredBooking(bkgId2);

        assertThat(meterRegistry.find("booking.expiration.sweep.count").counter()).isNotNull();
        assertThat(meterRegistry.find("booking.expiration.sweep.count").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("sweepExpiredBookings handles empty search results cleanly")
    void sweepExpiredBookings_Empty() {
        when(bookingRepository.findExpiredBookingsByStatuses(any(), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        bookingExpirationScheduler.sweepExpiredBookings();

        verify(expiredBookingProcessor, never()).processExpiredBooking(any());

        assertThat(meterRegistry.find("booking.expiration.sweep.count").counter()).isNotNull();
        assertThat(meterRegistry.find("booking.expiration.sweep.count").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("sweepExpiredBookings isolates processor exceptions per booking without aborting sweep")
    void sweepExpiredBookings_HandlesProcessorException() {
        Booking bkg1 = Booking.builder().id(bkgId1).bookingReference("REF1").build();
        Booking bkg2 = Booking.builder().id(bkgId2).bookingReference("REF2").build();

        when(bookingRepository.findExpiredBookingsByStatuses(any(), any(Instant.class)))
                .thenReturn(List.of(bkg1, bkg2));

        doThrow(new RuntimeException("Lock release error")).when(expiredBookingProcessor).processExpiredBooking(bkgId1);

        bookingExpirationScheduler.sweepExpiredBookings();

        verify(expiredBookingProcessor).processExpiredBooking(bkgId1);
        verify(expiredBookingProcessor).processExpiredBooking(bkgId2);
    }
}
