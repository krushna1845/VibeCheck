package com.krushna.moviebooking.booking.integration.security;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Milestone 13 — Test 11b: Role Authorization Tests
 *
 * <p>Verifies:
 * 1. Customer with ROLE_CUSTOMER attempting to access ADMIN endpoint (e.g. POST /api/v1/movies, POST /api/v1/theatres) -> 403 Forbidden.
 * 2. Admin with ROLE_ADMIN -> Successfully accesses admin endpoints.
 */
@DisplayName("M13-Security: Role-based Authorization Verification (CUSTOMER vs ADMIN)")
class RoleAuthorizationTest {

    @Test
    @Disabled("Run via milestone-13-verify.ps1 against live Gateway on port 8079")
    @DisplayName("[MANUAL] Customer role accessing Admin endpoint -> 403 Forbidden")
    void customerRole_adminEndpoint_forbidden() {
        // Verification script validates non-admin token receiving 403 on admin-only route
    }

    @Test
    @Disabled("Run via milestone-13-verify.ps1 against live Gateway on port 8079")
    @DisplayName("[MANUAL] Admin role accessing Admin endpoint -> Success")
    void adminRole_adminEndpoint_success() {
        // Verification script validates admin token receiving 201/200 on admin route
    }
}
