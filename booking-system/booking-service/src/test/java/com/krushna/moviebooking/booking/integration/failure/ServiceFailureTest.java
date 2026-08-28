package com.krushna.moviebooking.booking.integration.failure;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Milestone 13 — Test 8: Service Failure / Circuit Breaker (Gateway-level)
 *
 * <p>Tests that when a downstream service (show-service, payment-service, notification-service)
 * is stopped, the API Gateway circuit breaker:
 * 1. Retries the request (3 attempts, 500ms backoff)
 * 2. Opens the circuit breaker after 50% failure rate (5+ calls, 10-call window)
 * 3. Returns a fallback response (503 with meaningful message)
 * 4. Does NOT create partial booking data
 * 5. Recovers when the downstream service restarts (CB transitions HalfOpen → Closed)
 *
 * <p>These tests require a live Docker stack and cannot be run in isolation via Maven.
 * They are @Disabled and must be run via milestone-13-verify.ps1.
 *
 * <p>The Gateway Resilience4j configuration (from gateway-service/application.yml):
 * - slidingWindowSize: 10
 * - minimumNumberOfCalls: 5
 * - failureRateThreshold: 50.0
 * - waitDurationInOpenState: 5000ms
 * - maxAttempts (retry): 3
 * - waitDuration (retry): 500ms
 */
@DisplayName("M13-Resilience: Circuit breaker and service failure tests (Docker-level)")
class ServiceFailureTest {

    @Test
    @Disabled("Run via milestone-13-verify.ps1: docker compose stop show-service → verify CB opens → restart → verify recovery")
    @DisplayName("[MANUAL] docker compose stop show-service → CB OPEN → fallback 503 → restart → CB CLOSED")
    void showServiceDown_circuitBreakerOpensAndRecovers() {
        /*
         * Evidence flow (from milestone-13-verify.ps1):
         *
         * 1. docker compose stop show-service
         * 2. POST /api/v1/bookings → Resilience4j retries 3x
         * 3. After sufficient failures, CB transitions CLOSED → OPEN
         * 4. Response: HTTP 503 {"message":"show-service is currently unavailable..."}
         * 5. CB state visible at GET /actuator/health (circuitbreakers)
         * 6. docker compose start show-service
         * 7. CB transitions OPEN → HALF_OPEN → CLOSED after 3 probe requests
         * 8. POST /api/v1/bookings → succeeds normally
         */
    }

    @Test
    @Disabled("Run via milestone-13-verify.ps1: docker compose stop payment-service → verify CB opens")
    @DisplayName("[MANUAL] docker compose stop payment-service → CB OPEN for payment routes")
    void paymentServiceDown_circuitBreakerOpens() {
        /*
         * Evidence flow:
         * 1. docker compose stop payment-service
         * 2. POST /api/v1/payments → retries 3x
         * 3. CB OPEN → fallback: {"message":"payment-service is currently unavailable. Please try later."}
         * 4. Booking status remains PENDING (no partial state written)
         * 5. docker compose start payment-service → CB recovers
         */
    }

    @Test
    @Disabled("Run via milestone-13-verify.ps1: docker compose stop notification-service → booking still confirms")
    @DisplayName("[MANUAL] docker compose stop notification-service → booking confirms normally (async)")
    void notificationServiceDown_bookingStillConfirms() {
        /*
         * Notification service is consumed asynchronously via Kafka.
         * Stopping it does not affect booking confirmation.
         * When restarted, it will consume backlog from Kafka topic.
         */
    }
}
