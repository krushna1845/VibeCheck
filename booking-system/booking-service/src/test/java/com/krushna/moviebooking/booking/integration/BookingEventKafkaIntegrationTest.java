package com.krushna.moviebooking.booking.integration;

import com.krushna.moviebooking.booking.config.KafkaConfig;
import com.krushna.moviebooking.booking.event.*;
import com.krushna.moviebooking.booking.idempotency.IdempotencyService;
import com.krushna.moviebooking.booking.outbox.OutboxEventService;
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
class BookingEventKafkaIntegrationTest {

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private IdempotencyService idempotencyService;

    @Test
    @DisplayName("End-to-End Publishing and Idempotent Consumption Flow Integration")
    void testEndToEndPublishAndConsumeFlow() {
        KafkaBookingEventPublisher publisher = new KafkaBookingEventPublisher(outboxEventService);
        BookingEventConsumer consumer = new BookingEventConsumer(idempotencyService);

        UUID eventUuid = UUID.randomUUID();
        String eventId = eventUuid.toString();
        BookingCreatedEvent createdEvent = BookingCreatedEvent.builder()
                .eventId(eventId)
                .bookingId(UUID.randomUUID())
                .bookingReference("BKG-INT-001")
                .userId(UUID.randomUUID())
                .showId(UUID.randomUUID())
                .showSeatIds(List.of(UUID.randomUUID()))
                .totalAmount(new BigDecimal("180.00"))
                .expiresAt(Instant.now().plusSeconds(600))
                .timestamp(Instant.now())
                .build();

        // Step 1: Business Transaction writes Outbox record atomically (no direct Kafka call)
        publisher.publishBookingCreated(createdEvent);

        verify(outboxEventService).saveEvent(eq(eventUuid), eq("Booking"), eq("BKG-INT-001"), eq("BOOKING_CREATED"), eq(1), eq(createdEvent));

        // Step 2: First Consumption (Not Processed)
        when(idempotencyService.isEventProcessed(eventId)).thenReturn(false);

        consumer.consumeBookingCreated(createdEvent, KafkaConfig.BOOKING_CREATED_TOPIC, "BKG-INT-001");

        verify(idempotencyService).markEventAsProcessed(eq(eventId), eq("BOOKING_CREATED"), eq("booking-service-group"));

        // Step 3: Second Consumption (Duplicate - Processed)
        when(idempotencyService.isEventProcessed(eventId)).thenReturn(true);

        consumer.consumeBookingCreated(createdEvent, KafkaConfig.BOOKING_CREATED_TOPIC, "BKG-INT-001");

        // Verification: markEventAsProcessed was only invoked once (during first consumption)
        verify(idempotencyService, times(1)).markEventAsProcessed(anyString(), anyString(), anyString());
    }
}
