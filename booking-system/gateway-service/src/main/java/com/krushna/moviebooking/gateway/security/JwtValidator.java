package com.krushna.moviebooking.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Validates JWT tokens locally — avoids a round-trip to auth-service on every request.
 * The shared secret must exactly match the auth-service JWT secret.
 */
@Slf4j
@Component
public class JwtValidator {

    private final SecretKey key;

    private static final String DEFAULT_INSECURE_SECRET = "9a4f2c8d7e6b5a4c3f2e1d0c9b8a7f6e5d4c3b2a1f0e9d8c7b6a5f4e3d2c1b0a";

    @org.springframework.beans.factory.annotation.Autowired
    public JwtValidator(
            @Value("${jwt.secret:9a4f2c8d7e6b5a4c3f2e1d0c9b8a7f6e5d4c3b2a1f0e9d8c7b6a5f4e3d2c1b0a}") String secret,
            @Value("${spring.profiles.active:default}") String activeProfile) {
        if (DEFAULT_INSECURE_SECRET.equals(secret)) {
            if ("prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile)) {
                throw new IllegalStateException("FATAL: Hardcoded JWT secret detected at Gateway in production! Inject a secure secret via JWT_SECRET.");
            } else {
                log.warn("[SECURITY ALERT] Gateway is using default hardcoded JWT secret. DO NOT USE IN PRODUCTION!");
            }
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public JwtValidator(String secret) {
        this(secret, "default");
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("[Gateway] JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID getUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public String getEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        return parseClaims(token).get("roles", List.class);
    }
}
