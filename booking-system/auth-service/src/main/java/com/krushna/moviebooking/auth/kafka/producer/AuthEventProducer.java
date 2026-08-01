package com.krushna.moviebooking.auth.kafka.producer;

import com.krushna.moviebooking.auth.kafka.event.AuthEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes auth domain events to Kafka.
 * Topics consumed by: notification-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventProducer {

    private static final String AUTH_EVENTS_TOPIC = "auth.events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishAuthEvent(AuthEvent event) {
        log.info("[AuthEventProducer] Publishing event type='{}' userId={}", event.eventType(), event.userId());
        kafkaTemplate.send(AUTH_EVENTS_TOPIC, event.userId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[AuthEventProducer] Failed to publish event type='{}' for userId={}: {}",
                                event.eventType(), event.userId(), ex.getMessage());
                    } else {
                        log.debug("[AuthEventProducer] Event published successfully type='{}' partition={} offset={}",
                                event.eventType(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
