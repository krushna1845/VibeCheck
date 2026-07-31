package com.krushna.moviebooking.booking.event;

import com.krushna.moviebooking.booking.config.KafkaConfig;
import com.krushna.moviebooking.booking.idempotency.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Idempotent Kafka consumer listening for domain and dead letter queue (DLQ) events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventConsumer {

    private final IdempotencyService idempotencyService;

    @KafkaListener(topics = KafkaConfig.BOOKING_CREATED_TOPIC, groupId = "${spring.kafka.consumer.group-id:booking-service-group}")
    public void consumeBookingCreated(@Payload BookingCreatedEvent event,
                                      @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
                                      @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        String eventId = event.eventId();
        String eventType = event.eventType();
        log.info("[KafkaConsumer] Received BookingCreatedEvent | topic={} key={} eventId={} bookingRef={}",
                topic, key, eventId, event.bookingReference());

        if (idempotencyService.isEventProcessed(eventId)) {
            log.info("[KafkaConsumer] Skipping already processed event | eventId={}", eventId);
            return;
        }

        log.info("[KafkaConsumer] Successfully processed BookingCreatedEvent for bookingRef={}", event.bookingReference());
        idempotencyService.markEventAsProcessed(eventId, eventType, "booking-service-group");
    }

    @KafkaListener(topics = KafkaConfig.BOOKING_CONFIRMED_TOPIC, groupId = "${spring.kafka.consumer.group-id:booking-service-group}")
    public void consumeBookingConfirmed(@Payload BookingConfirmedEvent event,
                                        @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
                                        @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        String eventId = event.eventId();
        String eventType = event.eventType();
        log.info("[KafkaConsumer] Received BookingConfirmedEvent | topic={} key={} eventId={} bookingRef={}",
                topic, key, eventId, event.bookingReference());

        if (idempotencyService.isEventProcessed(eventId)) {
            log.info("[KafkaConsumer] Skipping already processed event | eventId={}", eventId);
            return;
        }

        log.info("[KafkaConsumer] Successfully processed BookingConfirmedEvent for bookingRef={}", event.bookingReference());
        idempotencyService.markEventAsProcessed(eventId, eventType, "booking-service-group");
    }

    @KafkaListener(topics = KafkaConfig.BOOKING_CANCELLED_TOPIC, groupId = "${spring.kafka.consumer.group-id:booking-service-group}")
    public void consumeBookingCancelled(@Payload BookingCancelledEvent event,
                                        @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
                                        @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        String eventId = event.eventId();
        String eventType = event.eventType();
        log.info("[KafkaConsumer] Received BookingCancelledEvent | topic={} key={} eventId={} bookingRef={}",
                topic, key, eventId, event.bookingReference());

        if (idempotencyService.isEventProcessed(eventId)) {
            log.info("[KafkaConsumer] Skipping already processed event | eventId={}", eventId);
            return;
        }

        log.info("[KafkaConsumer] Successfully processed BookingCancelledEvent for bookingRef={}", event.bookingReference());
        idempotencyService.markEventAsProcessed(eventId, eventType, "booking-service-group");
    }

    @KafkaListener(topics = KafkaConfig.BOOKING_EXPIRED_TOPIC, groupId = "${spring.kafka.consumer.group-id:booking-service-group}")
    public void consumeBookingExpired(@Payload BookingExpiredEvent event,
                                      @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
                                      @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        String eventId = event.eventId();
        String eventType = event.eventType();
        log.info("[KafkaConsumer] Received BookingExpiredEvent | topic={} key={} eventId={} bookingRef={}",
                topic, key, eventId, event.bookingReference());

        if (idempotencyService.isEventProcessed(eventId)) {
            log.info("[KafkaConsumer] Skipping already processed event | eventId={}", eventId);
            return;
        }

        log.info("[KafkaConsumer] Successfully processed BookingExpiredEvent for bookingRef={}", event.bookingReference());
        idempotencyService.markEventAsProcessed(eventId, eventType, "booking-service-group");
    }

    /**
     * Dead Letter Queue (DLQ) listener for handling failed messages routed to .DLT topics.
     */
    @KafkaListener(topics = {
            KafkaConfig.BOOKING_CREATED_DLT,
            KafkaConfig.BOOKING_CONFIRMED_DLT,
            KafkaConfig.BOOKING_CANCELLED_DLT,
            KafkaConfig.BOOKING_EXPIRED_DLT
    }, groupId = "${spring.kafka.consumer.group-id:booking-service-group}-dlt")
    public void consumeDeadLetterEvent(@Payload Object payload,
                                      @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
                                      @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        log.error("[KafkaDLQ] Received Dead-Letter record | topic={} key={} payload={}", topic, key, payload);
    }
}
