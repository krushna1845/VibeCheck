package com.krushna.moviebooking.auth.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AuthEventConsumer {
    @KafkaListener(topics = "default-topic")
    public void listen(String message) {
        // placeholder consumer
    }
}
