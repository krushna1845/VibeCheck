package com.krushna.moviebooking.booking.outbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link OutboxEvent} with multi-instance concurrency protection.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    @Query("SELECT o FROM OutboxEvent o WHERE (o.status = 'PENDING' OR o.status = 'FAILED') " +
           "AND o.retryCount < :maxRetries " +
           "AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= :now) " +
           "ORDER BY o.createdAt ASC, o.id ASC")
    List<OutboxEvent> findPendingOrRetryableEventsWithLock(
            @Param("maxRetries") int maxRetries,
            @Param("now") Instant now,
            Pageable pageable);

    @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'IN_PROGRESS' " +
           "AND (o.nextRetryAt IS NOT NULL AND o.nextRetryAt <= :now) " +
           "ORDER BY o.createdAt ASC, o.id ASC")
    List<OutboxEvent> findStaleInProgressEvents(@Param("now") Instant now, Pageable pageable);

    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(String status);

    List<OutboxEvent> findTop50ByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(List<String> statuses, int maxRetries);

    List<OutboxEvent> findByAggregateIdOrderByCreatedAtAsc(String aggregateId);
}

