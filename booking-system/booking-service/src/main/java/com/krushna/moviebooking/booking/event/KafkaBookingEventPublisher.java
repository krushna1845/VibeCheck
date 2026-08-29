package com.krushna.moviebooking.booking.event;

import com.krushna.moviebooking.booking.outbox.OutboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Primary transactional implementation of {@link BookingEventPublisher}.
 *
 * <p>Persists domain events to the outbox table within the caller's active database transaction.
 * Never performs synchronous or asynchronous Kafka network I/O directly, adhering strictly to
 * the Transactional Outbox pattern.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaBookingEventPublisher implements BookingEventPublisher {

    private final OutboxEventService outboxEventService;

    @Override
    public void publishBookingCreated(BookingCreatedEvent event) {
        log.info("[OutboxPublisher] Enqueuing BookingCreatedEvent to outbox | ref={} id={} version={}",
                event.bookingReference(), event.eventId(), event.eventVersion());
        UUID eventUuid = parseUuidSafely(event.eventId());
        outboxEventService.saveEvent(eventUuid, "Booking", event.bookingReference(), event.eventType(), event.eventVersion(), event);
    }

    @Override
    public void publishBookingConfirmed(BookingConfirmedEvent event) {
        log.info("[OutboxPublisher] Enqueuing BookingConfirmedEvent to outbox | ref={} id={} version={}",
                event.bookingReference(), event.eventId(), event.eventVersion());
        UUID eventUuid = parseUuidSafely(event.eventId());
        outboxEventService.saveEvent(eventUuid, "Booking", event.bookingReference(), event.eventType(), event.eventVersion(), event);
    }

    @Override
    public void publishBookingCancelled(BookingCancelledEvent event) {
        log.info("[OutboxPublisher] Enqueuing BookingCancelledEvent to outbox | ref={} id={} version={}",
                event.bookingReference(), event.eventId(), event.eventVersion());
        UUID eventUuid = parseUuidSafely(event.eventId());
        outboxEventService.saveEvent(eventUuid, "Booking", event.bookingReference(), event.eventType(), event.eventVersion(), event);
    }

    @Override
    public void publishBookingExpired(BookingExpiredEvent event) {
        log.info("[OutboxPublisher] Enqueuing BookingExpiredEvent to outbox | ref={} id={} version={}",
                event.bookingReference(), event.eventId(), event.eventVersion());
        UUID eventUuid = parseUuidSafely(event.eventId());
        outboxEventService.saveEvent(eventUuid, "Booking", event.bookingReference(), event.eventType(), event.eventVersion(), event);
    }

    @Override
    public void publishBookingFailed(BookingFailedEvent event) {
        log.info("[OutboxPublisher] Enqueuing BookingFailedEvent to outbox | ref={} id={} version={}",
                event.bookingReference(), event.eventId(), event.eventVersion());
        UUID eventUuid = parseUuidSafely(event.eventId());
        outboxEventService.saveEvent(eventUuid, "Booking", event.bookingReference(), event.eventType(), event.eventVersion(), event);
    }

    private UUID parseUuidSafely(String id) {
        if (id == null || id.isBlank()) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return UUID.randomUUID();
        }
    }
}
