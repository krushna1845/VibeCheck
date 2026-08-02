package com.krushna.moviebooking.notification.kafka;

import com.krushna.moviebooking.booking.event.BookingCancelledEvent;
import com.krushna.moviebooking.booking.event.BookingConfirmedEvent;
import com.krushna.moviebooking.notification.entity.NotificationChannelType;
import com.krushna.moviebooking.payment.event.PaymentFailedEvent;
import com.krushna.moviebooking.payment.event.PaymentSuccessEvent;
import com.krushna.moviebooking.notification.service.NotificationRequest;
import com.krushna.moviebooking.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class BookingEventConsumer {

    private final NotificationService notificationService;

    public BookingEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "booking-confirmed-events", groupId = "notification-service-group")
    public void handleBookingConfirmedEvent(BookingConfirmedEvent event) {
        NotificationRequest request = NotificationRequest.builder()
                .userId(event.userId())
                .recipient(resolveRecipient(event.userId()))
                .channelType(NotificationChannelType.EMAIL)
                .eventType("BOOKING_CONFIRMED")
                .templateKey("booking-confirmed")
                .subject("Booking confirmed")
                .content(null)
                .metadata(Map.of(
                        "bookingReference", event.bookingReference(),
                        "amount", event.totalAmount()
                ))
                .build();

        notificationService.sendNotification(request);
    }

    @KafkaListener(topics = "booking-cancelled-events", groupId = "notification-service-group")
    public void handleBookingCancelledEvent(BookingCancelledEvent event) {
        NotificationRequest request = NotificationRequest.builder()
                .userId(event.userId())
                .recipient(resolveRecipient(event.userId()))
                .channelType(NotificationChannelType.EMAIL)
                .eventType("BOOKING_CANCELLED")
                .templateKey("booking-cancelled")
                .subject("Booking cancelled")
                .content(null)
                .metadata(Map.of("bookingReference", event.bookingReference(), "reason", event.reason()))
                .build();

        notificationService.sendNotification(request);
    }

    @KafkaListener(topics = "payment-success-events", groupId = "notification-service-group")
    public void handlePaymentSuccessEvent(PaymentSuccessEvent event) {
        NotificationRequest request = NotificationRequest.builder()
                .userId(event.userId())
                .recipient(resolveRecipient(event.userId()))
                .channelType(NotificationChannelType.EMAIL)
                .eventType("PAYMENT_SUCCESS")
                .templateKey("payment-success")
                .subject("Payment successful")
                .content(null)
                .metadata(Map.of(
                        "bookingReference", event.bookingReference(),
                        "amount", event.amount()
                ))
                .build();

        notificationService.sendNotification(request);
    }

    @KafkaListener(topics = "payment-failed-events", groupId = "notification-service-group")
    public void handlePaymentFailedEvent(PaymentFailedEvent event) {
        NotificationRequest request = NotificationRequest.builder()
                .userId(event.userId())
                .recipient(resolveRecipient(event.userId()))
                .channelType(NotificationChannelType.EMAIL)
                .eventType("PAYMENT_FAILED")
                .templateKey("payment-failed")
                .subject("Payment failed")
                .content(null)
                .metadata(Map.of(
                        "bookingReference", event.bookingReference(),
                        "failureReason", event.failureReason()
                ))
                .build();

        notificationService.sendNotification(request);
    }

    private String resolveRecipient(UUID userId) {
        return userId != null ? userId + "@example.com" : "unknown@example.com";
    }
}
