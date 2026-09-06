package com.krushna.moviebooking.notification.kafka;

import com.krushna.moviebooking.common.event.BookingEvents.BookingCancelledEvent;
import com.krushna.moviebooking.common.event.BookingEvents.BookingConfirmedEvent;
import com.krushna.moviebooking.common.event.BookingEvents.BookingExpiredEvent;
import com.krushna.moviebooking.notification.entity.ProcessedEvent;
import com.krushna.moviebooking.notification.repository.ProcessedEventRepository;
import com.krushna.moviebooking.notification.service.NotificationRequest;
import com.krushna.moviebooking.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BookingEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private BookingEventConsumer consumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        consumer = new BookingEventConsumer(notificationService, processedEventRepository);
    }

    @Test
    void shouldHandleBookingConfirmedEvent() {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BookingConfirmedEvent event = new BookingConfirmedEvent(
                "evt-1", "BOOKING_CONFIRMED", 1, bookingId, "BK-1001",
                userId, UUID.randomUUID(), List.of(UUID.randomUUID()), List.of("A1", "A2"),
                new BigDecimal("500.00"), "PAY-123", Instant.now()
        );

        when(processedEventRepository.existsById("evt-1")).thenReturn(false);

        consumer.handleBookingConfirmedEvent(event);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).sendNotification(captor.capture());
        verify(processedEventRepository).save(any(ProcessedEvent.class));

        NotificationRequest request = captor.getValue();
        assertThat(request.userId()).isEqualTo(userId);
        assertThat(request.eventType()).isEqualTo("BOOKING_CONFIRMED");
        assertThat(request.templateKey()).isEqualTo("booking-confirmed");
        assertThat(request.metadata().get("bookingReference")).isEqualTo("BK-1001");
    }

    @Test
    void shouldSkipDuplicateBookingConfirmedEvent() {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BookingConfirmedEvent event = new BookingConfirmedEvent(
                "evt-dup", "BOOKING_CONFIRMED", 1, bookingId, "BK-1001",
                userId, UUID.randomUUID(), List.of(UUID.randomUUID()), List.of("A1", "A2"),
                new BigDecimal("500.00"), "PAY-123", Instant.now()
        );

        when(processedEventRepository.existsById("evt-dup")).thenReturn(true);

        consumer.handleBookingConfirmedEvent(event);

        verify(notificationService, never()).sendNotification(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void shouldHandleBookingCancelledEvent() {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BookingCancelledEvent event = new BookingCancelledEvent(
                "evt-2", "BOOKING_CANCELLED", 1, bookingId, "BK-1002",
                userId, UUID.randomUUID(), List.of(), "User requested cancellation", Instant.now()
        );

        when(processedEventRepository.existsById("evt-2")).thenReturn(false);

        consumer.handleBookingCancelledEvent(event);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).sendNotification(captor.capture());
        verify(processedEventRepository).save(any(ProcessedEvent.class));

        NotificationRequest request = captor.getValue();
        assertThat(request.userId()).isEqualTo(userId);
        assertThat(request.eventType()).isEqualTo("BOOKING_CANCELLED");
        assertThat(request.metadata().get("reason")).isEqualTo("User requested cancellation");
    }

    @Test
    void shouldHandleBookingExpiredEvent() {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BookingExpiredEvent event = new BookingExpiredEvent(
                "evt-3", "BOOKING_EXPIRED", 1, bookingId, "BK-1003",
                userId, UUID.randomUUID(), List.of(), Instant.now()
        );

        when(processedEventRepository.existsById("evt-3")).thenReturn(false);

        consumer.handleBookingExpiredEvent(event);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).sendNotification(captor.capture());
        verify(processedEventRepository).save(any(ProcessedEvent.class));

        NotificationRequest request = captor.getValue();
        assertThat(request.userId()).isEqualTo(userId);
        assertThat(request.eventType()).isEqualTo("BOOKING_EXPIRED");
    }
}
