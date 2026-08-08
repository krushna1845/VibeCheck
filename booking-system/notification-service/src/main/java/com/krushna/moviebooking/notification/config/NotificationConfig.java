package com.krushna.moviebooking.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(NotificationProperties.class)
@EnableJpaAuditing
@EnableRetry
@EnableScheduling
public class NotificationConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
