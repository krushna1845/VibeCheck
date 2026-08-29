package com.krushna.moviebooking.booking.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Transactional service for managing outbox events with multi-instance concurrency safety,
 * lease claiming, exponential backoff, and idempotent persistence.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private static final Duration DEFAULT_LEASE_DURATION = Duration.ofSeconds(60);
    private static final int BASE_BACKOFF_SECONDS = 2;
    private static final int MAX_BACKOFF_SECONDS = 300;

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Persists an outbox event within the active business database transaction.
     */
    @Transactional
    public <T> OutboxEvent saveEvent(String aggregateType, String aggregateId, String eventType, int eventVersion, T payload) {
        return saveEvent(null, aggregateType, aggregateId, eventType, eventVersion, payload);
    }

    /**
     * Persists an outbox event with an explicit idempotent eventId within the active business database transaction.
     */
    @Transactional
    public <T> OutboxEvent saveEvent(UUID eventId, String aggregateType, String aggregateId, String eventType, int eventVersion, T payload) {
        try {
            String jsonPayload;
            if (payload instanceof String str) {
                jsonPayload = str;
            } else {
                jsonPayload = objectMapper.writeValueAsString(payload);
            }

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(eventId != null ? eventId : UUID.randomUUID())
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .eventVersion(eventVersion)
                    .payload(jsonPayload)
                    .status(OutboxEvent.STATUS_PENDING)
                    .retryCount(0)
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

    /**
     * Claims a batch of pending or retryable events for processing using SELECT FOR UPDATE SKIP LOCKED.
     * Transitions claimed events to IN_PROGRESS and assigns a lease timeout to prevent multi-instance race conditions.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<OutboxEvent> claimEventsForProcessing(int maxRetries, int batchSize) {
        return claimEventsForProcessing(maxRetries, batchSize, DEFAULT_LEASE_DURATION);
    }

    /**
     * Claims a batch of pending or retryable events with a custom lease duration.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<OutboxEvent> claimEventsForProcessing(int maxRetries, int batchSize, Duration leaseDuration) {
        Instant now = Instant.now();

        // 1. Reclaim any expired in-progress leases
        reclaimStaleInProgressEvents(now);

        // 2. Lock and claim next batch of pending/failed events whose retry time is due
        List<OutboxEvent> eventsToClaim = outboxEventRepository.findPendingOrRetryableEventsWithLock(
                maxRetries, now, PageRequest.of(0, batchSize));

        if (eventsToClaim.isEmpty()) {
            return List.of();
        }

        Instant leaseExpiry = now.plus(leaseDuration);
        for (OutboxEvent event : eventsToClaim) {
            event.setStatus(OutboxEvent.STATUS_IN_PROGRESS);
            event.setNextRetryAt(leaseExpiry);
        }

        List<OutboxEvent> claimed = outboxEventRepository.saveAll(eventsToClaim);
        log.info("[OutboxService] Claimed {} outbox events for publishing (lease until: {})", claimed.size(), leaseExpiry);
        return claimed;
    }

    /**
     * Reclaims stale IN_PROGRESS events whose lease expired without being completed or marked failed.
     */
    private void reclaimStaleInProgressEvents(Instant now) {
        List<OutboxEvent> staleEvents = outboxEventRepository.findStaleInProgressEvents(now, PageRequest.of(0, 50));
        if (!staleEvents.isEmpty()) {
            log.warn("[OutboxService] Found {} stale IN_PROGRESS outbox events. Reclaiming back to PENDING.", staleEvents.size());
            for (OutboxEvent stale : staleEvents) {
                stale.setStatus(OutboxEvent.STATUS_PENDING);
                stale.setNextRetryAt(null);
            }
            outboxEventRepository.saveAll(staleEvents);
        }
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> fetchPendingEvents() {
        return outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEvent.STATUS_PENDING);
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> fetchPendingOrRetryableEvents(int maxRetries) {
        return outboxEventRepository.findTop50ByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(
                List.of(OutboxEvent.STATUS_PENDING, OutboxEvent.STATUS_FAILED), maxRetries);
    }

    /**
     * Marks an outbox event as PUBLISHED upon successful Kafka delivery in an isolated transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsPublished(UUID eventId) {
        outboxEventRepository.findById(eventId).ifPresent(this::markAsPublished);
    }

    /**
     * Marks an outbox event instance as PUBLISHED.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsPublished(OutboxEvent outboxEvent) {
        outboxEvent.setStatus(OutboxEvent.STATUS_PUBLISHED);
        outboxEvent.setProcessedAt(Instant.now());
        outboxEvent.setNextRetryAt(null);
        outboxEvent.setErrorMessage(null);
        outboxEventRepository.save(outboxEvent);
        log.info("[OutboxService] Marked outbox event as PUBLISHED | id={}", outboxEvent.getId());
    }

    /**
     * Marks an outbox event as FAILED upon publish failure, applying exponential backoff and recording the error.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(UUID eventId, String errorMessage, int maxRetries) {
        outboxEventRepository.findById(eventId).ifPresent(event -> markAsFailed(event, errorMessage, maxRetries));
    }

    /**
     * Marks an outbox event instance as FAILED.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(OutboxEvent outboxEvent, String errorMessage) {
        markAsFailed(outboxEvent, errorMessage, 5);
    }

    /**
     * Marks an outbox event instance as FAILED with explicit maxRetries threshold.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(OutboxEvent outboxEvent, String errorMessage, int maxRetries) {
        int newRetryCount = outboxEvent.getRetryCount() + 1;
        outboxEvent.setRetryCount(newRetryCount);
        outboxEvent.setErrorMessage(truncateError(errorMessage));

        if (newRetryCount >= maxRetries) {
            outboxEvent.setStatus(OutboxEvent.STATUS_DEAD_LETTER);
            outboxEvent.setNextRetryAt(null);
            log.error("[OutboxService] Outbox event reached max retries ({}/{}). Marked as DEAD_LETTER | id={} error={}",
                    newRetryCount, maxRetries, outboxEvent.getId(), errorMessage);
        } else {
            outboxEvent.setStatus(OutboxEvent.STATUS_FAILED);
            long backoffSeconds = calculateExponentialBackoff(newRetryCount);
            Instant nextRetry = Instant.now().plusSeconds(backoffSeconds);
            outboxEvent.setNextRetryAt(nextRetry);
            log.warn("[OutboxService] Outbox event failed (attempt {}/{}). Next retry in {}s at {} | id={} error={}",
                    newRetryCount, maxRetries, backoffSeconds, nextRetry, outboxEvent.getId(), errorMessage);
        }

        outboxEventRepository.save(outboxEvent);
    }

    private long calculateExponentialBackoff(int retryCount) {
        long delay = (long) Math.pow(BASE_BACKOFF_SECONDS, retryCount);
        return Math.min(delay, MAX_BACKOFF_SECONDS);
    }

    private String truncateError(String error) {
        if (error == null) {
            return null;
        }
        return error.length() > 3900 ? error.substring(0, 3900) + "...[truncated]" : error;
    }
}
