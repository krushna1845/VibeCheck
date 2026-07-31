package com.krushna.moviebooking.payment.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson {@link ObjectMapper} configuration for the Payment Service.
 *
 * <p>Registers the {@link JavaTimeModule} so that {@link java.time.Instant} fields
 * (used in all DTOs) serialize to ISO-8601 strings rather than numeric epoch arrays.
 * This is required for Redis idempotency serialization and Kafka event payloads.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper paymentObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
