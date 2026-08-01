package com.krushna.moviebooking.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private final String secret = "9a4f2c8d7e6b5a4c3f2e1d0c9b8a7f6e5d4c3b2a1f0e9d8c7b6a5f4e3d2c1b0a";

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(secret, 3600000L, 86400000L);
    }

    @Test
    void generateAccessToken_and_validate_Success() {
        UUID userId = UUID.randomUUID();
        String email = "john.doe@example.com";
        List<String> roles = List.of("ROLE_USER");

        String token = tokenProvider.generateAccessToken(userId, email, roles);

        assertThat(token).isNotBlank();
        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUserIdFromToken(token)).isEqualTo(userId.toString());
        assertThat(tokenProvider.getEmailFromToken(token)).isEqualTo(email);
        assertThat(tokenProvider.getRolesFromToken(token)).containsExactly("ROLE_USER");
    }

    @Test
    void validateToken_InvalidToken_ReturnsFalse() {
        assertThat(tokenProvider.validateToken("invalid.jwt.token")).isFalse();
    }
}
