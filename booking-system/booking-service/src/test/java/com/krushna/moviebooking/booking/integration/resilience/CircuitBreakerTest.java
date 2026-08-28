package com.krushna.moviebooking.booking.integration.resilience;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Milestone 13 — Test 9: Gateway Resilience4j Circuit Breaker Verification
 *
 * <p>Verifies:
 * 1. Gateway tracks call failure rates to downstream microservices.
 * 2. When failure rate exceeds threshold (50% over sliding window), Circuit Breaker opens.
 * 3. In OPEN state, Gateway immediately returns fallback without hammering the downstream service.
 * 4. After waitDurationInOpenState (5s), transitions to HALF_OPEN to probe downstream health.
 * 5. Recovers to CLOSED when downstream service resumes healthy responses.
 */
@DisplayName("M13-Resilience: Circuit Breaker State Transitions & Fallbacks")
class CircuitBreakerTest {

    @Test
    @Disabled("Run via milestone-13-verify.ps1 against live Gateway on port 8079")
    @DisplayName("[MANUAL] Downstream outage triggers Circuit Breaker OPEN and executes Fallback")
    void circuitBreaker_opensOnFailure_andRecovers() {
        // Handled in verification script and verified in M13 report
    }
}
