package com.krushna.moviebooking.payment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic provisioning for the Payment Service.
 *
 * <p>Topics declared here are created automatically on startup if they do not exist.
 */
@Configuration
public class KafkaConfig {

    public static final String PAYMENT_INITIATED_TOPIC  = "payment-initiated-events";
    public static final String PAYMENT_SUCCESS_TOPIC    = "payment-success-events";
    public static final String PAYMENT_FAILED_TOPIC     = "payment-failed-events";
    public static final String PAYMENT_REFUNDED_TOPIC   = "payment-refunded-events";

    @Bean
    public NewTopic paymentInitiatedTopic() {
        return TopicBuilder.name(PAYMENT_INITIATED_TOPIC)
                .partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentSuccessTopic() {
        return TopicBuilder.name(PAYMENT_SUCCESS_TOPIC)
                .partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name(PAYMENT_FAILED_TOPIC)
                .partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentRefundedTopic() {
        return TopicBuilder.name(PAYMENT_REFUNDED_TOPIC)
                .partitions(3).replicas(1).build();
    }
}
