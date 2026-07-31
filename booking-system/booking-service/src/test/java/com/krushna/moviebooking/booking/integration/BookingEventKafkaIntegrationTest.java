package com.krushna.moviebooking.booking.integration;

import com.krushna.moviebooking.booking.config.KafkaConfig;
import com.krushna.moviebooking.booking.event.*;
import com.krushna.moviebooking.booking.idempotency.IdempotencyService;
import com.krushna.moviebooking.booking.outbox.OutboxEventService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingEventKafkaIntegrationTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private IdempotencyService idempotencyService;

    @Test
    @DisplayName("End-to-End Publishing and Idempotent Consumption Flow Integration")
    void testEndToEndPublishAndConsumeFlow() {
        KafkaBookingEventPublisher publisher = new KafkaBookingEventPublisher(kafkaTemplate, outboxEventService);
        BookingEventConsumer consumer = new BookingEventConsumer(idempotencyService);

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        String eventId = UUID.randomUUID().toString();
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

        // Step 1: Publish Event
        publisher.publishBookingCreated(createdEvent);

        verify(outboxEventService).saveEvent(eq("Booking"), eq("BKG-INT-001"), eq("BOOKING_CREATED"), eq(1), eq(createdEvent));
        verify(kafkaTemplate).send(any(ProducerRecord.class));

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
