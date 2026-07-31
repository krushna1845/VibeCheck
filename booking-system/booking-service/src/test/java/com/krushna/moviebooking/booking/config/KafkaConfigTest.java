package com.krushna.moviebooking.booking.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaConfigTest {

    private final KafkaConfig kafkaConfig = new KafkaConfig();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Inject @Value fields that are not available outside a Spring context
        ReflectionTestUtils.setField(kafkaConfig, "bootstrapServers", "localhost:9092");
        ReflectionTestUtils.setField(kafkaConfig, "groupId", "booking-service-group");
    }

    @Test
    @DisplayName("Should provision booking topics with correct name and 3 partitions")
    void testTopicBeans() {
        NewTopic createdTopic = kafkaConfig.bookingCreatedTopic();
        assertThat(createdTopic.name()).isEqualTo(KafkaConfig.BOOKING_CREATED_TOPIC);
        assertThat(createdTopic.numPartitions()).isEqualTo(3);

        NewTopic confirmedTopic = kafkaConfig.bookingConfirmedTopic();
        assertThat(confirmedTopic.name()).isEqualTo(KafkaConfig.BOOKING_CONFIRMED_TOPIC);

        NewTopic cancelledTopic = kafkaConfig.bookingCancelledTopic();
        assertThat(cancelledTopic.name()).isEqualTo(KafkaConfig.BOOKING_CANCELLED_TOPIC);

        NewTopic expiredTopic = kafkaConfig.bookingExpiredTopic();
        assertThat(expiredTopic.name()).isEqualTo(KafkaConfig.BOOKING_EXPIRED_TOPIC);
    }

    @Test
    @DisplayName("Should provision Dead Letter Topics with correct names and 1 partition")
    void testDltTopicBeans() {
        NewTopic createdDlt = kafkaConfig.bookingCreatedDltTopic();
        assertThat(createdDlt.name()).isEqualTo(KafkaConfig.BOOKING_CREATED_DLT);
        assertThat(createdDlt.numPartitions()).isEqualTo(1);

        NewTopic confirmedDlt = kafkaConfig.bookingConfirmedDltTopic();
        assertThat(confirmedDlt.name()).isEqualTo(KafkaConfig.BOOKING_CONFIRMED_DLT);

        NewTopic cancelledDlt = kafkaConfig.bookingCancelledDltTopic();
        assertThat(cancelledDlt.name()).isEqualTo(KafkaConfig.BOOKING_CANCELLED_DLT);

        NewTopic expiredDlt = kafkaConfig.bookingExpiredDltTopic();
        assertThat(expiredDlt.name()).isEqualTo(KafkaConfig.BOOKING_EXPIRED_DLT);
    }

    @Test
    @DisplayName("Should configure ProducerFactory and KafkaTemplate")
    void testProducerFactoryAndTemplate() {
        ProducerFactory<String, Object> producerFactory = kafkaConfig.producerFactory(objectMapper);
        assertThat(producerFactory).isNotNull();

        KafkaTemplate<String, Object> kafkaTemplate = kafkaConfig.kafkaTemplate(producerFactory);
        assertThat(kafkaTemplate).isNotNull();
    }

    @Test
    @DisplayName("Should configure ConsumerFactory, ErrorHandler, and ListenerContainerFactory")
    void testConsumerAndErrorHandlerBeans() {
        ConsumerFactory<String, Object> consumerFactory = kafkaConfig.consumerFactory(objectMapper);
        assertThat(consumerFactory).isNotNull();

        ProducerFactory<String, Object> producerFactory = kafkaConfig.producerFactory(objectMapper);
        KafkaTemplate<String, Object> kafkaTemplate = kafkaConfig.kafkaTemplate(producerFactory);
        DeadLetterPublishingRecoverer recoverer = kafkaConfig.deadLetterPublishingRecoverer(kafkaTemplate);
        assertThat(recoverer).isNotNull();

        DefaultErrorHandler errorHandler = kafkaConfig.kafkaErrorHandler(recoverer);
        assertThat(errorHandler).isNotNull();

        ConcurrentKafkaListenerContainerFactory<String, Object> listenerContainerFactory =
                kafkaConfig.kafkaListenerContainerFactory(consumerFactory, errorHandler);
        assertThat(listenerContainerFactory).isNotNull();
    }
}
