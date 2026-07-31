package com.krushna.moviebooking.booking.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Entity tracking processed event IDs for consumer idempotency checks.
 */
@Entity
@Table(name = "processed_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "consumer_group", nullable = false, length = 100)
    @Builder.Default
    private String consumerGroup = "booking-service-group";

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;
}
