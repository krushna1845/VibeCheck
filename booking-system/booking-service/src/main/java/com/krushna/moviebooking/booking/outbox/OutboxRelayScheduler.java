package com.krushna.moviebooking.booking.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krushna.moviebooking.booking.config.KafkaConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Production-grade Outbox Relay Scheduler.
 *
 * <p>Periodically claims pending and retryable outbox events from the outbox_events table
 * using SELECT FOR UPDATE SKIP LOCKED and publishes them to Kafka to ensure transactional
 * outbox eventual delivery.
 *
 * <p>Key Guarantees:
 * 1. Non-blocking DB transactions: Claiming events, Kafka publishing, and status updating are executed
 *    in separate small transactions so database connection pools are never held during network I/O to Kafka.
 * 2. Multi-instance concurrency: Uses SELECT FOR UPDATE SKIP LOCKED and atomic IN_PROGRESS status transitions
 *    with lease timeouts so multiple replicas can poll concurrently without duplicate dispatches.
 * 3. Exponential backoff: Failed dispatches calculate progressive retry delays before next polling attempt.
 * 4. Graceful recovery: Stranded events automatically recover upon lease expiry.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private final OutboxEventService outboxEventService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Value("${booking.outbox.max-retries:5}")
    private int maxRetries = 5;

    @Value("${booking.outbox.batch-size:50}")
    private int batchSize = 50;

    @Value("${booking.outbox.publish-timeout-seconds:5}")
    private long publishTimeoutSeconds = 5;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private static final Map<String, String> EVENT_TOPIC_MAP = Map.of(
            "BOOKING_CREATED", KafkaConfig.BOOKING_CREATED_TOPIC,
            "BOOKING_CONFIRMED", KafkaConfig.BOOKING_CONFIRMED_TOPIC,
            "BOOKING_CANCELLED", KafkaConfig.BOOKING_CANCELLED_TOPIC,
            "BOOKING_EXPIRED", KafkaConfig.BOOKING_EXPIRED_TOPIC,
            "BOOKING_FAILED", KafkaConfig.BOOKING_FAILED_TOPIC
    );

    @Scheduled(fixedDelayString = "${booking.outbox.relay-interval-ms:5000}")
    public void processOutboxEvents() {
        if (!isRunning.compareAndSet(false, true)) {
            log.debug("[OutboxRelay] Skipping run as previous relay cycle is still active.");
            return;
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            List<OutboxEvent> claimedEvents = outboxEventService.claimEventsForProcessing(maxRetries, batchSize);
            if (claimedEvents == null || claimedEvents.isEmpty()) {
                return;
            }

            log.info("[OutboxRelay] Claimed {} pending/retryable outbox events to publish", claimedEvents.size());

            for (OutboxEvent event : claimedEvents) {
                processSingleEvent(event);
            }
        } catch (Exception e) {
            log.error("[OutboxRelay] Error during outbox relay execution cycle: {}", e.getMessage(), e);
        } finally {
            isRunning.set(false);
            sample.stop(Timer.builder("outbox.relay.duration")
                    .description("Time taken to process outbox relay cycle")
                    .register(meterRegistry));
        }
    }

    private void processSingleEvent(OutboxEvent event) {
        String topic = EVENT_TOPIC_MAP.get(event.getEventType());
        if (topic == null) {
            log.error("[OutboxRelay] Unknown eventType '{}' for outbox event id={}. Marking as failed.",
                    event.getEventType(), event.getId());
            outboxEventService.markAsFailed(event, "Unknown eventType: " + event.getEventType(), maxRetries);
            recordFailureMetric(event.getEventType());
            return;
        }

        try {
            Object payloadObject;
            try {
                payloadObject = objectMapper.readValue(event.getPayload(), Object.class);
            } catch (Exception parseEx) {
                payloadObject = event.getPayload();
            }

            ProducerRecord<String, Object> record = new ProducerRecord<>(topic, event.getAggregateId(), payloadObject);
            record.headers().add(new RecordHeader("eventId", event.getId().toString().getBytes(StandardCharsets.UTF_8)));
            record.headers().add(new RecordHeader("eventType", event.getEventType().getBytes(StandardCharsets.UTF_8)));

            kafkaTemplate.send(record).get(publishTimeoutSeconds, TimeUnit.SECONDS);

            outboxEventService.markAsPublished(event);

            Counter.builder("outbox.relay.processed")
                    .tag("eventType", event.getEventType())
                    .description("Count of outbox events successfully published to Kafka")
                    .register(meterRegistry)
                    .increment();

            log.info("[OutboxRelay] Successfully relayed outbox event id={} aggregateId={} topic={}",
                    event.getId(), event.getAggregateId(), topic);

        } catch (Exception e) {
            log.warn("[OutboxRelay] Failed to publish outbox event id={} aggregateId={} to topic={}. RetryCount={}. Error: {}",
                    event.getId(), event.getAggregateId(), topic, event.getRetryCount(), e.getMessage());

            outboxEventService.markAsFailed(event, e.getMessage(), maxRetries);
            recordFailureMetric(event.getEventType());
        }
    }

    private void recordFailureMetric(String eventType) {
        Counter.builder("outbox.relay.failed")
                .tag("eventType", eventType != null ? eventType : "UNKNOWN")
                .description("Count of outbox event publish failures")
                .register(meterRegistry)
                .increment();
    }
}
