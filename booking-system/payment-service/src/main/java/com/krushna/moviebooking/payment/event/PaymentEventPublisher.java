package com.krushna.moviebooking.payment.event;

import com.krushna.moviebooking.payment.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes payment domain events to Kafka topics.
 *
 * <p>Each method is fire-and-forget with structured logging for observability.
 * Kafka send failures are logged as errors but do not roll back the database transaction;
 * downstream consumers should treat all events as at-least-once.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes a {@link PaymentInitiatedEvent} to {@value KafkaConfig#PAYMENT_INITIATED_TOPIC}.
     *
     * @param event Initiation event
     */
    public void publishPaymentInitiated(PaymentInitiatedEvent event) {
        String topic = KafkaConfig.PAYMENT_INITIATED_TOPIC;
        log.info("[PaymentEvent] Publishing INITIATED | topic={} paymentId={} bookingRef={}",
                topic, event.paymentId(), event.bookingReference());
        kafkaTemplate.send(topic, event.paymentId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[PaymentEvent] Failed to publish INITIATED for paymentId={}: {}",
                                event.paymentId(), ex.getMessage());
                    } else {
                        log.debug("[PaymentEvent] INITIATED published to partition={} offset={}",
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    /**
     * Publishes a {@link PaymentSuccessEvent} to {@value KafkaConfig#PAYMENT_SUCCESS_TOPIC}.
     *
     * @param event Success event
     */
    public void publishPaymentSuccess(PaymentSuccessEvent event) {
        String topic = KafkaConfig.PAYMENT_SUCCESS_TOPIC;
        log.info("[PaymentEvent] Publishing SUCCESS | topic={} paymentId={} bookingRef={}",
                topic, event.paymentId(), event.bookingReference());
        kafkaTemplate.send(topic, event.paymentId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[PaymentEvent] Failed to publish SUCCESS for paymentId={}: {}",
                                event.paymentId(), ex.getMessage());
                    }
                });
    }

    /**
     * Publishes a {@link PaymentFailedEvent} to {@value KafkaConfig#PAYMENT_FAILED_TOPIC}.
     *
     * @param event Failure event
     */
    public void publishPaymentFailed(PaymentFailedEvent event) {
        String topic = KafkaConfig.PAYMENT_FAILED_TOPIC;
        log.info("[PaymentEvent] Publishing FAILED | topic={} paymentId={} bookingRef={} reason={}",
                topic, event.paymentId(), event.bookingReference(), event.failureReason());
        kafkaTemplate.send(topic, event.paymentId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[PaymentEvent] Failed to publish FAILED for paymentId={}: {}",
                                event.paymentId(), ex.getMessage());
                    }
                });
    }

    /**
     * Publishes a {@link PaymentRefundedEvent} to {@value KafkaConfig#PAYMENT_REFUNDED_TOPIC}.
     *
     * @param event Refunded event
     */
    public void publishPaymentRefunded(PaymentRefundedEvent event) {
        String topic = KafkaConfig.PAYMENT_REFUNDED_TOPIC;
        log.info("[PaymentEvent] Publishing REFUNDED | topic={} paymentId={} refundRef={}",
                topic, event.paymentId(), event.refundReference());
        kafkaTemplate.send(topic, event.paymentId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[PaymentEvent] Failed to publish REFUNDED for paymentId={}: {}",
                                event.paymentId(), ex.getMessage());
                    }
                });
    }
}
