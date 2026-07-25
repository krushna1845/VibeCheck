package com.krushna.moviebooking.gateway.kafka.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class GatewayEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public GatewayEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
}
