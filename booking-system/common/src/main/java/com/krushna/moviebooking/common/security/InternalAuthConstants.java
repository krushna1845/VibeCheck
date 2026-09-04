package com.krushna.moviebooking.common.security;

/**
 * Common security constants for perimeter and inter-service authentication.
 */
public final class InternalAuthConstants {

    private InternalAuthConstants() {}

    public static final String INTERNAL_SERVICE_HEADER = "X-Internal-Service";
    public static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLES_HEADER = "X-User-Roles";
    public static final String USER_EMAIL_HEADER = "X-User-Email";

    public static final String ROLE_INTERNAL_SERVICE = "ROLE_INTERNAL_SERVICE";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";
    public static final String ROLE_USER = "ROLE_USER";

    public static final String DEFAULT_INTERNAL_SECRET = "vibecheck-internal-perimeter-secret-key-m17";
}
