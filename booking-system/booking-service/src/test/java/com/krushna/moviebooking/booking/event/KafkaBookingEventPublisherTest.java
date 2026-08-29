package com.krushna.moviebooking.booking.event;

import com.krushna.moviebooking.booking.outbox.OutboxEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaBookingEventPublisherTest {

    @Mock
    private OutboxEventService outboxEventService;

    private KafkaBookingEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new KafkaBookingEventPublisher(outboxEventService);
    }

    @Test
    @DisplayName("publishBookingCreated should save outbox event atomically in DB transaction")
    void testPublishBookingCreated() {
        UUID eventUuid = UUID.randomUUID();
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .eventId(eventUuid.toString())
                .bookingId(UUID.randomUUID())
                .bookingReference("BKG-001")
                .userId(UUID.randomUUID())
                .showId(UUID.randomUUID())
                .showSeatIds(List.of(UUID.randomUUID()))
                .totalAmount(new BigDecimal("200.00"))
                .expiresAt(Instant.now().plusSeconds(600))
                .timestamp(Instant.now())
                .build();

        publisher.publishBookingCreated(event);

        verify(outboxEventService).saveEvent(eq(eventUuid), eq("Booking"), eq("BKG-001"), eq("BOOKING_CREATED"), eq(1), eq(event));
    }

    @Test
    @DisplayName("publishBookingConfirmed should save outbox event atomically in DB transaction")
    void testPublishBookingConfirmed() {
        UUID eventUuid = UUID.randomUUID();
        BookingConfirmedEvent event = BookingConfirmedEvent.builder()
                .eventId(eventUuid.toString())
                .bookingId(UUID.randomUUID())
                .bookingReference("BKG-002")
                .userId(UUID.randomUUID())
                .showId(UUID.randomUUID())
                .showSeatIds(List.of(UUID.randomUUID()))
                .seatNumbers(List.of("B2"))
                .totalAmount(new BigDecimal("250.00"))
                .paymentId("PAY-123")
                .timestamp(Instant.now())
                .build();

        publisher.publishBookingConfirmed(event);

        verify(outboxEventService).saveEvent(eq(eventUuid), eq("Booking"), eq("BKG-002"), eq("BOOKING_CONFIRMED"), eq(1), eq(event));
    }

    @Test
    @DisplayName("publishBookingCancelled should save outbox event atomically in DB transaction")
    void testPublishBookingCancelled() {
        UUID eventUuid = UUID.randomUUID();
        BookingCancelledEvent event = BookingCancelledEvent.builder()
                .eventId(eventUuid.toString())
                .bookingId(UUID.randomUUID())
                .bookingReference("BKG-003")
                .userId(UUID.randomUUID())
                .showId(UUID.randomUUID())
                .showSeatIds(List.of(UUID.randomUUID()))
                .reason("User cancelled")
                .timestamp(Instant.now())
                .build();

        publisher.publishBookingCancelled(event);

        verify(outboxEventService).saveEvent(eq(eventUuid), eq("Booking"), eq("BKG-003"), eq("BOOKING_CANCELLED"), eq(1), eq(event));
    }

    @Test
    @DisplayName("publishBookingExpired should save outbox event atomically in DB transaction")
    void testPublishBookingExpired() {
        UUID eventUuid = UUID.randomUUID();
        BookingExpiredEvent event = BookingExpiredEvent.builder()
                .eventId(eventUuid.toString())
                .bookingId(UUID.randomUUID())
                .bookingReference("BKG-004")
                .userId(UUID.randomUUID())
                .showId(UUID.randomUUID())
                .showSeatIds(List.of(UUID.randomUUID()))
                .timestamp(Instant.now())
                .build();

        publisher.publishBookingExpired(event);

        verify(outboxEventService).saveEvent(eq(eventUuid), eq("Booking"), eq("BKG-004"), eq("BOOKING_EXPIRED"), eq(1), eq(event));
    }

    @Test
    @DisplayName("publishBookingFailed should save outbox event atomically in DB transaction")
    void testPublishBookingFailed() {
        UUID eventUuid = UUID.randomUUID();
        BookingFailedEvent event = BookingFailedEvent.builder()
                .eventId(eventUuid.toString())
                .bookingId(UUID.randomUUID())
                .bookingReference("BKG-005")
                .userId(UUID.randomUUID())
                .showId(UUID.randomUUID())
                .showSeatIds(List.of(UUID.randomUUID()))
                .failureReason("Payment timeout")
                .timestamp(Instant.now())
                .build();

        publisher.publishBookingFailed(event);

        verify(outboxEventService).saveEvent(eq(eventUuid), eq("Booking"), eq("BKG-005"), eq("BOOKING_FAILED"), eq(1), eq(event));
    }
}
