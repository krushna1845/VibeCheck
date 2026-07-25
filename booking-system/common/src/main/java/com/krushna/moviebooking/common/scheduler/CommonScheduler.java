package com.krushna.moviebooking.common.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CommonScheduler {
    @Scheduled(cron = "0 * * * * *")
    public void run() {
        // placeholder scheduled job
    }
}
