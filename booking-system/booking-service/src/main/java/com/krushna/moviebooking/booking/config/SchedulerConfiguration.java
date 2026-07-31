package com.krushna.moviebooking.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.TaskScheduler;

/**
 * Spring configuration enabling background scheduled tasks, retry capabilities,
 * and configuring a dedicated thread pool for execution.
 */
@Configuration
@EnableScheduling
@EnableRetry
public class SchedulerConfiguration {

    /**
     * Creates a dedicated {@link TaskScheduler} thread pool for booking background tasks.
     *
     * @return configured ThreadPoolTaskScheduler instance
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("booking-scheduler-");
        scheduler.setErrorHandler(throwable ->
                org.slf4j.LoggerFactory.getLogger(SchedulerConfiguration.class)
                        .error("Unexpected error occurred in scheduled task execution", throwable));
        scheduler.initialize();
        return scheduler;
    }
}
