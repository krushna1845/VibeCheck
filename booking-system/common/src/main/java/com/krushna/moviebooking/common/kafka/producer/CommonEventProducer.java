package com.krushna.moviebooking.common.kafka.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class CommonEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CommonEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
}
