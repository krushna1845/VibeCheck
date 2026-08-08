package com.krushna.moviebooking.common.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {
    // Observability is configured via Spring Boot Actuator auto-configuration
    // and application-observability.yml properties
    // No manual configuration needed for Brave/Zipkin integration
}
