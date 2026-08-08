package com.krushna.moviebooking.gateway.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service managing Micrometer metrics for gateway request counts, latencies, and circuit breaker fallbacks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayMetricsService {

    private final MeterRegistry meterRegistry;

    public void recordRequest(String serviceName, int statusCode, long durationMs) {
        Counter.builder("gateway.requests.total")
                .tag("service", serviceName)
                .tag("status", String.valueOf(statusCode))
                .description("Total requests processed by gateway")
                .register(meterRegistry)
                .increment();

        Timer.builder("gateway.requests.duration")
                .tag("service", serviceName)
                .tag("status", String.valueOf(statusCode))
                .description("Gateway request handling duration")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordFallback(String serviceName, String reason) {
        Counter.builder("gateway.fallback.total")
                .tag("service", serviceName)
                .tag("reason", reason)
                .description("Total fallback triggers by gateway")
                .register(meterRegistry)
                .increment();
    }
}
