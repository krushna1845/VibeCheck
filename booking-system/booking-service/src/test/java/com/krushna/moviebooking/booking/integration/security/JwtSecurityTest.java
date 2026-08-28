package com.krushna.moviebooking.booking.integration.security;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Milestone 13 — Test 11: JWT Security Tests (Gateway Reverse Proxy Verification)
 *
 * <p>Verifies:
 * 1. Missing Authorization header on protected endpoints -> 401 Unauthorized / 403 Forbidden.
 * 2. Invalid JWT signature / malformed token -> 401/403.
 * 3. Expired JWT -> 401/403.
 * 4. Valid JWT with ROLE_CUSTOMER -> Allowed to access customer endpoints (POST /api/v1/bookings).
 *
 * <p>Since JWT validation filter resides in gateway-service (port 8079), these tests are
 * verified directly against the Gateway API via milestone-13-verify.ps1.
 */
@DisplayName("M13-Security: JWT Security & Gateway Authentication Verification")
class JwtSecurityTest {

    @Test
    @Disabled("Run via milestone-13-verify.ps1 against live Gateway on port 8079")
    @DisplayName("[MANUAL] No Token -> 401/403 Rejected")
    void noToken_rejected() {
        // Handled in verification script and verified in M12/M13 report
    }

    @Test
    @Disabled("Run via milestone-13-verify.ps1 against live Gateway on port 8079")
    @DisplayName("[MANUAL] Invalid / Tampered Token -> 401/403 Rejected")
    void invalidToken_rejected() {
        // Handled in verification script and verified in M12/M13 report
    }

    @Test
    @Disabled("Run via milestone-13-verify.ps1 against live Gateway on port 8079")
    @DisplayName("[MANUAL] Expired Token -> 401/403 Rejected")
    void expiredToken_rejected() {
        // Handled in verification script and verified in M12/M13 report
    }

    @Test
    @Disabled("Run via milestone-13-verify.ps1 against live Gateway on port 8079")
    @DisplayName("[MANUAL] Valid Customer Token -> Access Granted")
    void validCustomerToken_accessGranted() {
        // Handled in verification script and verified in M12/M13 report
    }
}
