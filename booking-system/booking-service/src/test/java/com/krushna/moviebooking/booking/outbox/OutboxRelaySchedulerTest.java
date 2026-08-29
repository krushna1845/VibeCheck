package com.krushna.moviebooking.booking.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelaySchedulerTest {

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private OutboxRelayScheduler outboxRelayScheduler;

    @BeforeEach
    void setUp() {
        outboxRelayScheduler = new OutboxRelayScheduler(
                outboxEventService, kafkaTemplate, objectMapper, meterRegistry
        );
    }

    @Test
    @DisplayName("processOutboxEvents publishes claimed outbox events to Kafka and marks as PUBLISHED")
    void processOutboxEvents_Success() {
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("Booking")
                .aggregateId("BKG-1001")
                .eventType("BOOKING_CREATED")
                .eventVersion(1)
                .payload("{\"bookingReference\":\"BKG-1001\"}")
                .status("IN_PROGRESS")
                .retryCount(0)
                .createdAt(Instant.now())
                .build();

        when(outboxEventService.claimEventsForProcessing(anyInt(), anyInt())).thenReturn(List.of(event));

        RecordMetadata metadata = new RecordMetadata(new TopicPartition("booking-created-events", 0), 0, 0, 0, 0, 0);
        SendResult<String, Object> sendResult = new SendResult<>(null, metadata);
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        outboxRelayScheduler.processOutboxEvents();

        verify(kafkaTemplate).send(any(ProducerRecord.class));
        verify(outboxEventService).markAsPublished(event);
        assertThat(meterRegistry.find("outbox.relay.processed").counter()).isNotNull();
        assertThat(meterRegistry.find("outbox.relay.processed").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("processOutboxEvents handles Kafka publish errors and marks as FAILED")
    void processOutboxEvents_KafkaFailure() {
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("Booking")
                .aggregateId("BKG-1002")
                .eventType("BOOKING_CONFIRMED")
                .eventVersion(1)
                .payload("{\"bookingReference\":\"BKG-1002\"}")
                .status("IN_PROGRESS")
                .retryCount(0)
                .createdAt(Instant.now())
                .build();

        when(outboxEventService.claimEventsForProcessing(anyInt(), anyInt())).thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka cluster unreachable"));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(failedFuture);

        outboxRelayScheduler.processOutboxEvents();

        verify(outboxEventService).markAsFailed(eq(event), anyString(), eq(5));
        assertThat(meterRegistry.find("outbox.relay.failed").counter()).isNotNull();
        assertThat(meterRegistry.find("outbox.relay.failed").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("processOutboxEvents handles unknown event type gracefully")
    void processOutboxEvents_UnknownEventType() {
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("Booking")
                .aggregateId("BKG-1003")
                .eventType("UNKNOWN_EVENT_TYPE")
                .eventVersion(1)
                .payload("{}")
                .status("IN_PROGRESS")
                .retryCount(0)
                .createdAt(Instant.now())
                .build();

        when(outboxEventService.claimEventsForProcessing(anyInt(), anyInt())).thenReturn(List.of(event));

        outboxRelayScheduler.processOutboxEvents();

        verify(kafkaTemplate, never()).send(any(ProducerRecord.class));
        verify(outboxEventService).markAsFailed(eq(event), contains("Unknown eventType"), eq(5));
    }
}
