package com.krushna.moviebooking.booking.config;

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
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Configuration for Kafka topics, Producer, Consumer, Retry, and Dead Letter Queue (DLQ).
 */
@EnableKafka
@Configuration
public class KafkaConfig {

    public static final String BOOKING_CREATED_TOPIC = "booking-created-events";
    public static final String BOOKING_CONFIRMED_TOPIC = "booking-confirmed-events";
    public static final String BOOKING_CANCELLED_TOPIC = "booking-cancelled-events";
    public static final String BOOKING_EXPIRED_TOPIC = "booking-expired-events";
    public static final String BOOKING_FAILED_TOPIC = "booking-failed-events";

    public static final String BOOKING_CREATED_DLT = "booking-created-events.DLT";
    public static final String BOOKING_CONFIRMED_DLT = "booking-confirmed-events.DLT";
    public static final String BOOKING_CANCELLED_DLT = "booking-cancelled-events.DLT";
    public static final String BOOKING_EXPIRED_DLT = "booking-expired-events.DLT";

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:booking-service-group}")
    private String groupId;

    // -------------------------------------------------------------------------
    // TOPIC PROVISIONING
    // -------------------------------------------------------------------------

    @Bean
    public NewTopic bookingCreatedTopic() {
        return TopicBuilder.name(BOOKING_CREATED_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic bookingConfirmedTopic() {
        return TopicBuilder.name(BOOKING_CONFIRMED_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic bookingCancelledTopic() {
        return TopicBuilder.name(BOOKING_CANCELLED_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic bookingExpiredTopic() {
        return TopicBuilder.name(BOOKING_EXPIRED_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic bookingFailedTopic() {
        return TopicBuilder.name(BOOKING_FAILED_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic bookingCreatedDltTopic() {
        return TopicBuilder.name(BOOKING_CREATED_DLT).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic bookingConfirmedDltTopic() {
        return TopicBuilder.name(BOOKING_CONFIRMED_DLT).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic bookingCancelledDltTopic() {
        return TopicBuilder.name(BOOKING_CANCELLED_DLT).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic bookingExpiredDltTopic() {
        return TopicBuilder.name(BOOKING_EXPIRED_DLT).partitions(1).replicas(1).build();
    }

    // -------------------------------------------------------------------------
    // PRODUCER CONFIGURATION
    // -------------------------------------------------------------------------

    @Bean
    public ProducerFactory<String, Object> producerFactory(ObjectMapper objectMapper) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        JsonSerializer<Object> jsonSerializer = new JsonSerializer<>(objectMapper);
        jsonSerializer.setAddTypeInfo(true);

        return new DefaultKafkaProducerFactory<>(configProps, new StringSerializer(), jsonSerializer);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    // -------------------------------------------------------------------------
    // CONSUMER CONFIGURATION
    // -------------------------------------------------------------------------

    @Bean
    public ConsumerFactory<String, Object> consumerFactory(ObjectMapper objectMapper) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>(objectMapper);
        jsonDeserializer.addTrustedPackages("com.krushna.moviebooking.*", "java.util", "java.lang");
        jsonDeserializer.setUseTypeHeaders(true);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new ErrorHandlingDeserializer<>(new StringDeserializer()),
                new ErrorHandlingDeserializer<>(jsonDeserializer)
        );
    }

    // -------------------------------------------------------------------------
    // RETRY AND DEAD LETTER QUEUE (DLQ) CONFIGURATION
    // -------------------------------------------------------------------------

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaTemplate<String, Object> kafkaTemplate) {
        return new DeadLetterPublishingRecoverer(kafkaTemplate);
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        // Retry 3 attempts with 1000ms fixed backoff before sending to DLT
        org.springframework.util.backoff.FixedBackOff fixedBackOff = new org.springframework.util.backoff.FixedBackOff(1000L, 2L);
        return new DefaultErrorHandler(recoverer, fixedBackOff);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }
}
