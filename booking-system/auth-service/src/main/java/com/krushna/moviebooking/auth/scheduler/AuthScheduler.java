package com.krushna.moviebooking.auth.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuthScheduler {
    @Scheduled(cron = "0 * * * * *")
    public void run() {
        // placeholder scheduled job
    }
}
