package com.krushna.moviebooking.booking.event;

import com.krushna.moviebooking.booking.config.KafkaConfig;
import com.krushna.moviebooking.booking.outbox.OutboxEventService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class KafkaBookingEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private OutboxEventService outboxEventService;

    private KafkaBookingEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new KafkaBookingEventPublisher(kafkaTemplate, outboxEventService);

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        lenient().when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);
    }

    @Test
    @DisplayName("publishBookingCreated should save outbox event and send message to Kafka")
    void testPublishBookingCreated() {
        BookingCreatedEvent event = BookingCreatedEvent.builder()
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

        verify(outboxEventService).saveEvent(eq("Booking"), eq("BKG-001"), eq("BOOKING_CREATED"), eq(1), eq(event));

        ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());

        ProducerRecord<String, Object> record = captor.getValue();
        assertThat(record.topic()).isEqualTo(KafkaConfig.BOOKING_CREATED_TOPIC);
        assertThat(record.key()).isEqualTo("BKG-001");
        assertThat(record.value()).isEqualTo(event);
    }

    @Test
    @DisplayName("publishBookingConfirmed should save outbox event and send to Kafka")
    void testPublishBookingConfirmed() {
        BookingConfirmedEvent event = BookingConfirmedEvent.builder()
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

        verify(outboxEventService).saveEvent(eq("Booking"), eq("BKG-002"), eq("BOOKING_CONFIRMED"), eq(1), eq(event));

        ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        assertThat(captor.getValue().topic()).isEqualTo(KafkaConfig.BOOKING_CONFIRMED_TOPIC);
    }

    @Test
    @DisplayName("publishBookingCancelled should save outbox event and send to Kafka")
    void testPublishBookingCancelled() {
        BookingCancelledEvent event = BookingCancelledEvent.builder()
                .bookingId(UUID.randomUUID())
                .bookingReference("BKG-003")
                .userId(UUID.randomUUID())
                .showId(UUID.randomUUID())
                .showSeatIds(List.of(UUID.randomUUID()))
                .reason("User cancelled")
                .timestamp(Instant.now())
                .build();

        publisher.publishBookingCancelled(event);

        verify(outboxEventService).saveEvent(eq("Booking"), eq("BKG-003"), eq("BOOKING_CANCELLED"), eq(1), eq(event));
    }

    @Test
    @DisplayName("publishBookingExpired should save outbox event and send to Kafka")
    void testPublishBookingExpired() {
        BookingExpiredEvent event = BookingExpiredEvent.builder()
                .bookingId(UUID.randomUUID())
                .bookingReference("BKG-004")
                .userId(UUID.randomUUID())
                .showId(UUID.randomUUID())
                .showSeatIds(List.of(UUID.randomUUID()))
                .timestamp(Instant.now())
                .build();

        publisher.publishBookingExpired(event);

        verify(outboxEventService).saveEvent(eq("Booking"), eq("BKG-004"), eq("BOOKING_EXPIRED"), eq(1), eq(event));
    }
}
