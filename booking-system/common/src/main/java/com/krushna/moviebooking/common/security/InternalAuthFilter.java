package com.krushna.moviebooking.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Filter that authenticates internal service-to-service calls and Gateway-forwarded requests.
 *
 * <p>Security model:
 * 1. If {@code X-Internal-Secret} matches the configured cluster secret, the request is trusted.
 *    - If {@code X-User-Id} is present (forwarded from Gateway), the user's identity and roles are populated.
 *    - If {@code X-User-Id} is absent (service-to-service call), the caller identity from {@code X-Internal-Service}
 *      is populated with {@code ROLE_INTERNAL_SERVICE} and {@code ROLE_ADMIN}.
 * 2. If {@code X-Internal-Secret} is absent or invalid:
 *    - Incoming {@code X-User-Id} and {@code X-User-Roles} headers are STRICTLY IGNORED (anti-spoofing).
 *    - Fallback: if a valid Bearer JWT is provided and {@link JwtClaims} is configured, authenticate via JWT.
 * 3. Unauthenticated requests proceed with an empty {@link SecurityContextHolder}, allowing public endpoints
 *    or triggering 401/403 on protected routes.
 */
public class InternalAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(InternalAuthFilter.class);

    private final String expectedSecret;
    private final JwtClaims jwtClaims;

    public InternalAuthFilter(String expectedSecret) {
        this(expectedSecret, null);
    }

    public InternalAuthFilter(String expectedSecret, JwtClaims jwtClaims) {
        if (InternalAuthConstants.DEFAULT_INTERNAL_SECRET.equals(expectedSecret)) {
            String activeProfile = System.getProperty("spring.profiles.active");
            if (activeProfile == null) {
                activeProfile = System.getenv("SPRING_PROFILES_ACTIVE");
            }
            if ("prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile)) {
                throw new IllegalStateException("FATAL: Default internal perimeter secret detected in production! Inject a secure secret via INTERNAL_SECURITY_SECRET.");
            } else {
                log.warn("[SECURITY ALERT] Using default internal perimeter secret. DO NOT USE IN PRODUCTION!");
            }
        }
        this.expectedSecret = expectedSecret;
        this.jwtClaims = jwtClaims;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String internalSecret = request.getHeader(InternalAuthConstants.INTERNAL_SECRET_HEADER);
        String internalService = request.getHeader(InternalAuthConstants.INTERNAL_SERVICE_HEADER);
        String userId = request.getHeader(InternalAuthConstants.USER_ID_HEADER);
        String userRoles = request.getHeader(InternalAuthConstants.USER_ROLES_HEADER);

        if (StringUtils.hasText(internalSecret) && internalSecret.equals(expectedSecret)) {
            // Trusted internal cluster communication
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority(InternalAuthConstants.ROLE_INTERNAL_SERVICE));

            String principal;
            if (StringUtils.hasText(userId)) {
                // Gateway-forwarded user request
                principal = userId.trim();
                if (StringUtils.hasText(userRoles)) {
                    Arrays.stream(userRoles.split(","))
                            .map(String::trim)
                            .filter(StringUtils::hasText)
                            .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                            .map(SimpleGrantedAuthority::new)
                            .forEach(authorities::add);
                }
                log.debug("[InternalAuth] Trusted Gateway request: userId={}, roles={}", principal, authorities);
            } else {
                // Direct peer service-to-service call
                principal = StringUtils.hasText(internalService) ? internalService.trim() : "internal-service";
                authorities.add(new SimpleGrantedAuthority(InternalAuthConstants.ROLE_ADMIN));
                log.debug("[InternalAuth] Trusted Service-to-Service call from: {}", principal);
            }

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

        } else {
            // Not a trusted internal call. Explicitly IGNORE any spoofed X-User-Id / X-User-Roles headers.
            if (StringUtils.hasText(userId) || StringUtils.hasText(userRoles)) {
                log.warn("[InternalAuth] Untrusted request attempted to supply identity headers: uri={}", request.getRequestURI());
            }

            // Fallback: Validate Bearer token if present and JwtClaims configured
            String bearerToken = extractBearerToken(request);
            if (jwtClaims != null && StringUtils.hasText(bearerToken) && jwtClaims.isValid(bearerToken)) {
                try {
                    String sub = jwtClaims.getUserId(bearerToken).toString();
                    List<String> roles = jwtClaims.getRoles(bearerToken);
                    List<SimpleGrantedAuthority> authorities = roles != null
                            ? roles.stream().map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r).map(SimpleGrantedAuthority::new).toList()
                            : List.of();

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(sub, null, authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("[InternalAuth] Authenticated via Bearer token: userId={}", sub);
                } catch (Exception ex) {
                    log.warn("[InternalAuth] Error parsing JWT claims: {}", ex.getMessage());
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }
}
