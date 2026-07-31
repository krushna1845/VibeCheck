package com.krushna.moviebooking.booking.event;

import com.krushna.moviebooking.booking.config.KafkaConfig;
import com.krushna.moviebooking.booking.idempotency.IdempotencyService;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingEventConsumerTest {

    @Mock
    private IdempotencyService idempotencyService;

    private BookingEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new BookingEventConsumer(idempotencyService);
    }

    @Test
    @DisplayName("Should process BookingCreatedEvent when not previously processed")
    void testConsumeBookingCreatedNewEvent() {
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .eventId("EVT-101")
                .bookingId(UUID.randomUUID())
                .bookingReference("BKG-NEW-1")
                .userId(UUID.randomUUID())
                .showId(UUID.randomUUID())
                .showSeatIds(List.of(UUID.randomUUID()))
                .totalAmount(new BigDecimal("150.00"))
                .expiresAt(Instant.now().plusSeconds(600))
                .timestamp(Instant.now())
                .build();

        when(idempotencyService.isEventProcessed("EVT-101")).thenReturn(false);

        consumer.consumeBookingCreated(event, KafkaConfig.BOOKING_CREATED_TOPIC, "BKG-NEW-1");

        verify(idempotencyService).isEventProcessed("EVT-101");
        verify(idempotencyService).markEventAsProcessed(eq("EVT-101"), eq("BOOKING_CREATED"), eq("booking-service-group"));
    }

    @Test
    @DisplayName("Should skip BookingCreatedEvent when already processed")
    void testConsumeBookingCreatedDuplicateEvent() {
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .eventId("EVT-101")
                .bookingId(UUID.randomUUID())
                .bookingReference("BKG-NEW-1")
                .userId(UUID.randomUUID())
                .showId(UUID.randomUUID())
                .showSeatIds(List.of(UUID.randomUUID()))
                .totalAmount(new BigDecimal("150.00"))
                .expiresAt(Instant.now().plusSeconds(600))
                .timestamp(Instant.now())
                .build();

        when(idempotencyService.isEventProcessed("EVT-101")).thenReturn(true);

        consumer.consumeBookingCreated(event, KafkaConfig.BOOKING_CREATED_TOPIC, "BKG-NEW-1");

        verify(idempotencyService).isEventProcessed("EVT-101");
        verify(idempotencyService, never()).markEventAsProcessed(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should process BookingConfirmedEvent when not previously processed")
    void testConsumeBookingConfirmed() {
        BookingConfirmedEvent event = BookingConfirmedEvent.builder()
                .eventId("EVT-202")
                .bookingId(UUID.randomUUID())
                .bookingReference("BKG-CONF-1")
                .userId(UUID.randomUUID())
                .showId(UUID.randomUUID())
                .showSeatIds(List.of(UUID.randomUUID()))
                .seatNumbers(List.of("C1"))
                .totalAmount(new BigDecimal("200.00"))
                .paymentId("PAY-777")
                .timestamp(Instant.now())
                .build();

        when(idempotencyService.isEventProcessed("EVT-202")).thenReturn(false);

        consumer.consumeBookingConfirmed(event, KafkaConfig.BOOKING_CONFIRMED_TOPIC, "BKG-CONF-1");

        verify(idempotencyService).markEventAsProcessed(eq("EVT-202"), eq("BOOKING_CONFIRMED"), eq("booking-service-group"));
    }

    @Test
    @DisplayName("Should process Dead Letter Queue messages cleanly")
    void testConsumeDeadLetterEvent() {
        consumer.consumeDeadLetterEvent("Failed record payload", KafkaConfig.BOOKING_CREATED_DLT, "BKG-FAIL-1");
    }
}
