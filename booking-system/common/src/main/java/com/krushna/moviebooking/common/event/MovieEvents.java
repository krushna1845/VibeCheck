package com.krushna.moviebooking.common.event;

import java.time.Instant;
import java.util.UUID;

public class MovieEvents {

    public record MovieUpdatedEvent(
            String eventId,
            String eventType,
            Integer eventVersion,
            UUID movieId,
            String title,
            Instant timestamp
    ) {}
}
