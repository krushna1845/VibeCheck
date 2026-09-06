package com.krushna.moviebooking.notification.kafka;

import com.krushna.moviebooking.common.event.BookingEvents.BookingCancelledEvent;
import com.krushna.moviebooking.common.event.BookingEvents.BookingConfirmedEvent;
import com.krushna.moviebooking.common.event.BookingEvents.BookingExpiredEvent;
import com.krushna.moviebooking.notification.entity.NotificationChannelType;
import com.krushna.moviebooking.notification.entity.ProcessedEvent;
import com.krushna.moviebooking.notification.repository.ProcessedEventRepository;
import com.krushna.moviebooking.notification.service.NotificationRequest;
import com.krushna.moviebooking.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Component
public class BookingEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(BookingEventConsumer.class);

    private final NotificationService notificationService;
    private final ProcessedEventRepository processedEventRepository;

    public BookingEventConsumer(NotificationService notificationService,
                                ProcessedEventRepository processedEventRepository) {
        this.notificationService = notificationService;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    @KafkaListener(topics = "booking-confirmed-events", groupId = "notification-service-group")
    public void handleBookingConfirmedEvent(BookingConfirmedEvent event) {
        String eventId = event.eventId();
        if (eventId != null && processedEventRepository.existsById(eventId)) {
            log.warn("[IDEMPOTENCY] Duplicate BookingConfirmedEvent detected for eventId={}, reference={}. Skipping.",
                    eventId, event.bookingReference());
            return;
        }

        log.info("Processing BookingConfirmedEvent for eventId={}, bookingId={}, reference={}",
                eventId, event.bookingId(), event.bookingReference());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("bookingId", event.bookingId());
        metadata.put("bookingReference", event.bookingReference());
        metadata.put("amount", event.totalAmount() != null ? event.totalAmount().toString() : "0.00");
        metadata.put("seatNumbers", event.seatNumbers() != null ? String.join(", ", event.seatNumbers()) : "N/A");
        metadata.put("paymentId", event.paymentId() != null ? event.paymentId() : "N/A");
        metadata.put("showTitle", "Movie Show");
        metadata.put("venue", "Cinema Theater");

        NotificationRequest request = NotificationRequest.builder()
                .userId(event.userId())
                .recipient(null) // Will be resolved by UserServiceClient
                .channelType(NotificationChannelType.EMAIL)
                .eventType("BOOKING_CONFIRMED")
                .templateKey("booking-confirmed")
                .subject("Booking Confirmed - " + event.bookingReference())
                .metadata(metadata)
                .build();

        notificationService.sendNotification(request);

        if (eventId != null) {
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .eventType("BOOKING_CONFIRMED")
                    .consumerGroup("notification-service-group")
                    .build());
        }
    }

    @Transactional
    @KafkaListener(topics = "booking-cancelled-events", groupId = "notification-service-group")
    public void handleBookingCancelledEvent(BookingCancelledEvent event) {
        String eventId = event.eventId();
        if (eventId != null && processedEventRepository.existsById(eventId)) {
            log.warn("[IDEMPOTENCY] Duplicate BookingCancelledEvent detected for eventId={}, reference={}. Skipping.",
                    eventId, event.bookingReference());
            return;
        }

        log.info("Processing BookingCancelledEvent for eventId={}, bookingId={}, reference={}",
                eventId, event.bookingId(), event.bookingReference());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("bookingId", event.bookingId());
        metadata.put("bookingReference", event.bookingReference());
        metadata.put("reason", event.reason() != null ? event.reason() : "Cancelled by user");

        NotificationRequest request = NotificationRequest.builder()
                .userId(event.userId())
                .recipient(null)
                .channelType(NotificationChannelType.EMAIL)
                .eventType("BOOKING_CANCELLED")
                .templateKey("booking-cancelled")
                .subject("Booking Cancelled - " + event.bookingReference())
                .metadata(metadata)
                .build();

        notificationService.sendNotification(request);

        if (eventId != null) {
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .eventType("BOOKING_CANCELLED")
                    .consumerGroup("notification-service-group")
                    .build());
        }
    }

    @Transactional
    @KafkaListener(topics = "booking-expired-events", groupId = "notification-service-group")
    public void handleBookingExpiredEvent(BookingExpiredEvent event) {
        String eventId = event.eventId();
        if (eventId != null && processedEventRepository.existsById(eventId)) {
            log.warn("[IDEMPOTENCY] Duplicate BookingExpiredEvent detected for eventId={}, reference={}. Skipping.",
                    eventId, event.bookingReference());
            return;
        }

        log.info("Processing BookingExpiredEvent for eventId={}, bookingId={}, reference={}",
                eventId, event.bookingId(), event.bookingReference());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("bookingId", event.bookingId());
        metadata.put("bookingReference", event.bookingReference());

        NotificationRequest request = NotificationRequest.builder()
                .userId(event.userId())
                .recipient(null)
                .channelType(NotificationChannelType.EMAIL)
                .eventType("BOOKING_EXPIRED")
                .templateKey("booking-expired")
                .subject("Booking Expired - " + event.bookingReference())
                .metadata(metadata)
                .build();

        notificationService.sendNotification(request);

        if (eventId != null) {
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .eventType("BOOKING_EXPIRED")
                    .consumerGroup("notification-service-group")
                    .build());
        }
    }
}
