package com.krushna.moviebooking.auth.kafka.event;

import lombok.Builder;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Kafka event emitted when a user registers or logs in.
 * Consumed by notification-service.
 */
@Builder
public record AuthEvent(
        String eventId,
        String eventType,    // USER_REGISTERED | USER_LOGGED_IN | USER_LOGGED_OUT
        UUID userId,
        String email,
        String firstName,
        Set<String> roles,
        String source,
        Instant occurredAt
) {}
