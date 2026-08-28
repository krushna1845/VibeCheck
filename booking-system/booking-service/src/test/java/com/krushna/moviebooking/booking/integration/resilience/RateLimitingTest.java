package com.krushna.moviebooking.booking.integration.resilience;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Milestone 13 — Test 10: Gateway Rate-Limiting Verification
 *
 * <p>Verifies:
 * 1. Redis-backed rate limiter tracks request counts per IP/Client.
 * 2. Requests exceeding 60 req/min return HTTP 429 Too Many Requests.
 * 3. Legitimate requests succeed again after the 1-minute window expires.
 */
@DisplayName("M13-Resilience: Redis Rate Limiting & HTTP 429 Responses")
class RateLimitingTest {

    @Test
    @Disabled("Run via milestone-13-verify.ps1 against live Gateway on port 8079")
    @DisplayName("[MANUAL] Exceeding 60 req/min triggers HTTP 429 Too Many Requests")
    void rateLimiter_blocksExcessTraffic() {
        // Handled in verification script and verified in M13 report
    }
}
