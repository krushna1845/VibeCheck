package com.krushna.moviebooking.movie.event;

import com.krushna.moviebooking.common.event.MovieEvents;
import com.krushna.moviebooking.movie.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MovieEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishMovieUpdatedEvent(UUID movieId, String title) {
        String eventId = UUID.randomUUID().toString();
        MovieEvents.MovieUpdatedEvent event = new MovieEvents.MovieUpdatedEvent(
                eventId,
                "MOVIE_UPDATED",
                1,
                movieId,
                title,
                Instant.now()
        );

        log.info("Publishing MovieUpdatedEvent for movieId: {}, title: {}", movieId, title);
        try {
            kafkaTemplate.send(KafkaConfig.MOVIE_UPDATED_TOPIC, movieId.toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish MovieUpdatedEvent for movieId: {}", movieId, e);
        }
    }
}
