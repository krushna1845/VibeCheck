package com.krushna.moviebooking.common.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CommonEventConsumer {
    @KafkaListener(topics = "default-topic")
    public void listen(String message) {
        // placeholder consumer
    }
}
