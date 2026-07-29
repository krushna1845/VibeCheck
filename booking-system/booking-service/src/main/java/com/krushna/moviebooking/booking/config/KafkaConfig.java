package com.krushna.moviebooking.booking.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Spring Configuration for Kafka topic provisioning.
 */
@Configuration
public class KafkaConfig {

    public static final String BOOKING_CONFIRMED_TOPIC = "booking-confirmed-events";
    public static final String BOOKING_CANCELLED_TOPIC = "booking-cancelled-events";
    public static final String BOOKING_EXPIRED_TOPIC = "booking-expired-events";

    @Bean
    public NewTopic bookingConfirmedTopic() {
        return TopicBuilder.name(BOOKING_CONFIRMED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic bookingCancelledTopic() {
        return TopicBuilder.name(BOOKING_CANCELLED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic bookingExpiredTopic() {
        return TopicBuilder.name(BOOKING_EXPIRED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
