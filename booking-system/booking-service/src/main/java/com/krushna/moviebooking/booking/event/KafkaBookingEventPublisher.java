package com.krushna.moviebooking.booking.event;

import com.krushna.moviebooking.booking.config.KafkaConfig;
import com.krushna.moviebooking.booking.outbox.OutboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Primary implementation of {@link BookingEventPublisher} using Spring KafkaTemplate and Outbox-ready architecture.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaBookingEventPublisher implements BookingEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OutboxEventService outboxEventService;

    @Override
    public void publishBookingCreated(BookingCreatedEvent event) {
        log.info("[KafkaPublisher] Publishing BookingCreatedEvent | ref={} id={} version={}",
                event.bookingReference(), event.eventId(), event.eventVersion());
        outboxEventService.saveEvent("Booking", event.bookingReference(), event.eventType(), event.eventVersion(), event);
        sendEvent(KafkaConfig.BOOKING_CREATED_TOPIC, event.bookingReference(), event.eventId(), event.eventType(), event);
    }

    @Override
    public void publishBookingConfirmed(BookingConfirmedEvent event) {
        log.info("[KafkaPublisher] Publishing BookingConfirmedEvent | ref={} id={} version={}",
                event.bookingReference(), event.eventId(), event.eventVersion());
        outboxEventService.saveEvent("Booking", event.bookingReference(), event.eventType(), event.eventVersion(), event);
        sendEvent(KafkaConfig.BOOKING_CONFIRMED_TOPIC, event.bookingReference(), event.eventId(), event.eventType(), event);
    }

    @Override
    public void publishBookingCancelled(BookingCancelledEvent event) {
        log.info("[KafkaPublisher] Publishing BookingCancelledEvent | ref={} id={} version={}",
                event.bookingReference(), event.eventId(), event.eventVersion());
        outboxEventService.saveEvent("Booking", event.bookingReference(), event.eventType(), event.eventVersion(), event);
        sendEvent(KafkaConfig.BOOKING_CANCELLED_TOPIC, event.bookingReference(), event.eventId(), event.eventType(), event);
    }

    @Override
    public void publishBookingExpired(BookingExpiredEvent event) {
        log.info("[KafkaPublisher] Publishing BookingExpiredEvent | ref={} id={} version={}",
                event.bookingReference(), event.eventId(), event.eventVersion());
        outboxEventService.saveEvent("Booking", event.bookingReference(), event.eventType(), event.eventVersion(), event);
        sendEvent(KafkaConfig.BOOKING_EXPIRED_TOPIC, event.bookingReference(), event.eventId(), event.eventType(), event);
    }

    @Override
    public void publishBookingFailed(BookingFailedEvent event) {
        log.info("[KafkaPublisher] Publishing BookingFailedEvent | ref={} id={} version={}",
                event.bookingReference(), event.eventId(), event.eventVersion());
        outboxEventService.saveEvent("Booking", event.bookingReference(), event.eventType(), event.eventVersion(), event);
        sendEvent(KafkaConfig.BOOKING_FAILED_TOPIC, event.bookingReference(), event.eventId(), event.eventType(), event);
    }

    private void sendEvent(String topic, String key, String eventId, String eventType, Object payload) {
        try {
            ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, payload);
            if (eventId != null) {
                record.headers().add(new RecordHeader("eventId", eventId.getBytes(StandardCharsets.UTF_8)));
            }
            if (eventType != null) {
                record.headers().add(new RecordHeader("eventType", eventType.getBytes(StandardCharsets.UTF_8)));
            }

            kafkaTemplate.send(record).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[KafkaPublisher] Failed to send event to Kafka topic: {} with key: {} eventId: {}",
                            topic, key, eventId, ex);
                } else {
                    log.debug("[KafkaPublisher] Sent event to topic: {} partition: {} offset: {}",
                            topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("[KafkaPublisher] Synchronous error sending event to Kafka topic: {} with key: {}", topic, key, e);
        }
    }
}
