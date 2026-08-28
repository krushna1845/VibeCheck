package com.krushna.moviebooking.show.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka topic provisioning, producer, and consumer configuration for Show Service.
 *
 * <p>The Show Service acts as a <em>consumer</em> of booking and movie events so it
 * can invalidate its Redis caches whenever seats change state or movie metadata is
 * updated.
 */
@EnableKafka
@Configuration
public class ShowKafkaConfig {

    // Booking event topics the show-service subscribes to for cache invalidation
    public static final String BOOKING_CREATED_TOPIC   = "booking-created-events";
    public static final String BOOKING_CONFIRMED_TOPIC = "booking-confirmed-events";
    public static final String BOOKING_CANCELLED_TOPIC = "booking-cancelled-events";
    public static final String BOOKING_EXPIRED_TOPIC   = "booking-expired-events";

    // Movie event topic for movie update cache invalidation
    public static final String MOVIE_UPDATED_TOPIC = "movie-updated-events";

    public static final String CONSUMER_GROUP_ID = "show-service-cache-group";

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // -------------------------------------------------------------------------
    // CONSUMER CONFIGURATION
    // -------------------------------------------------------------------------

    @Bean
    public ConsumerFactory<String, Object> showConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP_ID);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.krushna.moviebooking.*");
        props.put(JsonDeserializer.TYPE_MAPPINGS, "com.krushna.moviebooking.booking.event.BookingCreatedEvent:com.krushna.moviebooking.common.event.BookingEvents$BookingCreatedEvent,com.krushna.moviebooking.booking.event.BookingConfirmedEvent:com.krushna.moviebooking.common.event.BookingEvents$BookingConfirmedEvent,com.krushna.moviebooking.booking.event.BookingCancelledEvent:com.krushna.moviebooking.common.event.BookingEvents$BookingCancelledEvent,com.krushna.moviebooking.booking.event.BookingExpiredEvent:com.krushna.moviebooking.common.event.BookingEvents$BookingExpiredEvent,com.krushna.moviebooking.movie.event.MovieUpdatedEvent:com.krushna.moviebooking.common.event.MovieEvents$MovieUpdatedEvent");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public DefaultErrorHandler showKafkaErrorHandler() {
        org.springframework.util.backoff.FixedBackOff backOff =
                new org.springframework.util.backoff.FixedBackOff(1000L, 2L);
        return new DefaultErrorHandler(backOff);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> showKafkaListenerContainerFactory(
            ConsumerFactory<String, Object> showConsumerFactory,
            DefaultErrorHandler showKafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(showConsumerFactory);
        factory.setCommonErrorHandler(showKafkaErrorHandler);
        return factory;
    }

    // -------------------------------------------------------------------------
    // PRODUCER CONFIGURATION (for publishing show events if needed)
    // -------------------------------------------------------------------------

    @Bean
    public ProducerFactory<String, Object> showProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> showKafkaTemplate(
            ProducerFactory<String, Object> showProducerFactory) {
        return new KafkaTemplate<>(showProducerFactory);
    }

    // -------------------------------------------------------------------------
    // TOPIC PROVISIONING
    // -------------------------------------------------------------------------

    @Bean
    public NewTopic showMovieUpdatedTopicDeclaration() {
        return TopicBuilder.name(MOVIE_UPDATED_TOPIC).partitions(3).replicas(1).build();
    }
}
