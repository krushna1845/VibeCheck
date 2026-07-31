package com.krushna.moviebooking.booking.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Transactional service for managing outbox events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public <T> OutboxEvent saveEvent(String aggregateType, String aggregateId, String eventType, int eventVersion, T payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .eventVersion(eventVersion)
                    .payload(jsonPayload)
                    .status("PENDING")
                    .build();
            OutboxEvent saved = outboxEventRepository.save(outboxEvent);
            log.info("[OutboxService] Saved outbox event | id={} aggregateType={} aggregateId={} eventType={}",
                    saved.getId(), aggregateType, aggregateId, eventType);
            return saved;
        } catch (Exception e) {
            log.error("[OutboxService] Failed to serialize and save outbox event for aggregateId={}", aggregateId, e);
            throw new RuntimeException("Outbox serialization failure", e);
        }
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> fetchPendingEvents() {
        return outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
    }

    @Transactional
    public void markAsPublished(OutboxEvent outboxEvent) {
        outboxEvent.setStatus("PUBLISHED");
        outboxEvent.setProcessedAt(Instant.now());
        outboxEventRepository.save(outboxEvent);
        log.info("[OutboxService] Marked outbox event as PUBLISHED | id={}", outboxEvent.getId());
    }

    @Transactional
    public void markAsFailed(OutboxEvent outboxEvent, String errorMessage) {
        outboxEvent.setStatus("FAILED");
        outboxEvent.setRetryCount(outboxEvent.getRetryCount() + 1);
        outboxEvent.setErrorMessage(errorMessage);
        outboxEventRepository.save(outboxEvent);
        log.error("[OutboxService] Marked outbox event as FAILED | id={} error={}", outboxEvent.getId(), errorMessage);
    }
}
