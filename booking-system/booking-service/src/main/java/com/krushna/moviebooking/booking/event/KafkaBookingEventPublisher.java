package com.krushna.moviebooking.booking.event;

import com.krushna.moviebooking.booking.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Primary implementation of {@link BookingEventPublisher} using Spring KafkaTemplate.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaBookingEventPublisher implements BookingEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishBookingCreated(BookingCreatedEvent event) {
        log.info("Publishing BookingCreatedEvent for reference: {}", event.bookingReference());
        sendEvent("booking-created-events", event.bookingReference(), event);
    }

    @Override
    public void publishBookingConfirmed(BookingConfirmedEvent event) {
        log.info("Publishing BookingConfirmedEvent for reference: {}", event.bookingReference());
        sendEvent(KafkaConfig.BOOKING_CONFIRMED_TOPIC, event.bookingReference(), event);
    }

    @Override
    public void publishBookingCancelled(BookingCancelledEvent event) {
        log.info("Publishing BookingCancelledEvent for reference: {}", event.bookingReference());
        sendEvent(KafkaConfig.BOOKING_CANCELLED_TOPIC, event.bookingReference(), event);
    }

    @Override
    public void publishBookingExpired(BookingExpiredEvent event) {
        log.info("Publishing BookingExpiredEvent for reference: {}", event.bookingReference());
        sendEvent(KafkaConfig.BOOKING_EXPIRED_TOPIC, event.bookingReference(), event);
    }

    @Override
    public void publishBookingFailed(BookingFailedEvent event) {
        log.info("Publishing BookingFailedEvent for reference: {}", event.bookingReference());
        sendEvent("booking-failed-events", event.bookingReference(), event);
    }

    private void sendEvent(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, payload);
        } catch (Exception e) {
            log.error("Failed to send event to Kafka topic: {} with key: {}", topic, key, e);
        }
    }
}
