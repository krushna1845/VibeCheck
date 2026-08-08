package com.krushna.moviebooking.notification.kafka;

import com.krushna.moviebooking.common.event.PaymentEvents.PaymentFailedEvent;
import com.krushna.moviebooking.common.event.PaymentEvents.PaymentSuccessEvent;
import com.krushna.moviebooking.notification.entity.NotificationChannelType;
import com.krushna.moviebooking.notification.service.NotificationRequest;
import com.krushna.moviebooking.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final NotificationService notificationService;

    public PaymentEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "payment-success-events", groupId = "notification-service-group")
    public void handlePaymentSuccessEvent(PaymentSuccessEvent event) {
        log.info("Received PaymentSuccessEvent for paymentId={}, reference={}", event.paymentId(), event.bookingReference());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("paymentId", event.paymentId());
        metadata.put("bookingReference", event.bookingReference());
        metadata.put("amount", event.amount() != null ? event.amount().toString() : "0.00");
        metadata.put("transactionReference", event.transactionReference() != null ? event.transactionReference() : "N/A");

        NotificationRequest request = NotificationRequest.builder()
                .userId(event.userId())
                .recipient(null)
                .channelType(NotificationChannelType.EMAIL)
                .eventType("PAYMENT_SUCCESS")
                .templateKey("payment-success")
                .subject("Payment Successful - " + event.bookingReference())
                .metadata(metadata)
                .build();

        notificationService.sendNotification(request);
    }

    @KafkaListener(topics = "payment-failed-events", groupId = "notification-service-group")
    public void handlePaymentFailedEvent(PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent for paymentId={}, reference={}", event.paymentId(), event.bookingReference());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("paymentId", event.paymentId());
        metadata.put("bookingReference", event.bookingReference());
        metadata.put("amount", event.amount() != null ? event.amount().toString() : "0.00");
        metadata.put("failureReason", event.failureReason() != null ? event.failureReason() : "Payment processing error");

        NotificationRequest request = NotificationRequest.builder()
                .userId(event.userId())
                .recipient(null)
                .channelType(NotificationChannelType.EMAIL)
                .eventType("PAYMENT_FAILED")
                .templateKey("payment-failed")
                .subject("Payment Failed - " + event.bookingReference())
                .metadata(metadata)
                .build();

        notificationService.sendNotification(request);
    }
}
