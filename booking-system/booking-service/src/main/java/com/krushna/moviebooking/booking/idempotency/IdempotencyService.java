package com.krushna.moviebooking.booking.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service enforcing event consumption idempotency.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedEventRepository processedEventRepository;

    /**
     * Checks if the event with given eventId has already been processed.
     */
    @Transactional(readOnly = true)
    public boolean isEventProcessed(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        boolean exists = processedEventRepository.existsById(eventId);
        if (exists) {
            log.info("[IdempotencyService] Duplicate event detected | eventId={}", eventId);
        }
        return exists;
    }

    /**
     * Records the eventId as processed to ensure future duplicate deliveries are skipped.
     */
    @Transactional
    public void markEventAsProcessed(String eventId, String eventType, String consumerGroup) {
        if (eventId == null || eventId.isBlank()) {
            return;
        }
        ProcessedEvent processedEvent = ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(eventType != null ? eventType : "UNKNOWN")
                .consumerGroup(consumerGroup != null ? consumerGroup : "booking-service-group")
                .build();
        processedEventRepository.save(processedEvent);
        log.info("[IdempotencyService] Recorded event as processed | eventId={} eventType={}", eventId, eventType);
    }
}
