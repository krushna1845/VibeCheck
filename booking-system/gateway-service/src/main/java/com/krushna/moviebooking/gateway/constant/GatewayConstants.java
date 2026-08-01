package com.krushna.moviebooking.gateway.constant;

/**
 * Common header constants injected by the gateway for downstream services.
 * These headers carry authenticated user context and eliminate the need for
 * each service to re-validate the JWT.
 */
public final class GatewayConstants {

    private GatewayConstants() {}

    /** Authenticated user's UUID */
    public static final String HEADER_USER_ID = "X-User-Id";

    /** Authenticated user's email address */
    public static final String HEADER_USER_EMAIL = "X-User-Email";

    /** Comma-separated list of RBAC roles, e.g. "ROLE_USER,ROLE_ADMIN" */
    public static final String HEADER_USER_ROLES = "X-User-Roles";

    /** Unique request trace ID for distributed tracing correlation */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    /** Name of this gateway, forwarded to downstream services */
    public static final String GATEWAY_NAME = "movie-booking-gateway";

    /** Kafka topics */
    public static final String TOPIC_GATEWAY_EVENTS = "gateway.events";
}
