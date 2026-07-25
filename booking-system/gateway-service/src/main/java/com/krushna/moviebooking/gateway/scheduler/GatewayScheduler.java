package com.krushna.moviebooking.gateway.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GatewayScheduler {
    @Scheduled(cron = "0 * * * * *")
    public void run() {
        // placeholder scheduled job
    }
}
