package com.krushna.moviebooking.booking.scheduler;

import com.krushna.moviebooking.booking.config.SchedulerConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerConfigurationTest {

    @Test
    @DisplayName("taskScheduler creates and initializes ThreadPoolTaskScheduler bean properly")
    void taskScheduler_BeanInitialization() {
        SchedulerConfiguration config = new SchedulerConfiguration();
        TaskScheduler taskScheduler = config.taskScheduler();

        assertThat(taskScheduler).isNotNull();
        assertThat(taskScheduler).isInstanceOf(ThreadPoolTaskScheduler.class);

        ThreadPoolTaskScheduler threadPoolTaskScheduler = (ThreadPoolTaskScheduler) taskScheduler;
        assertThat(threadPoolTaskScheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(5);
        assertThat(threadPoolTaskScheduler.getThreadNamePrefix()).isEqualTo("booking-scheduler-");

        threadPoolTaskScheduler.shutdown();
    }
}
