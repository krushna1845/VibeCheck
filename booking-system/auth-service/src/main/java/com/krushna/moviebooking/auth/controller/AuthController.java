package com.krushna.moviebooking.auth.controller;

import com.krushna.moviebooking.auth.dto.request.LoginRequest;
import com.krushna.moviebooking.auth.dto.request.RefreshTokenRequest;
import com.krushna.moviebooking.auth.dto.request.RegisterRequest;
import com.krushna.moviebooking.auth.dto.request.ValidateTokenRequest;
import com.krushna.moviebooking.auth.dto.response.AuthResponse;
import com.krushna.moviebooking.auth.dto.response.TokenValidationResponse;
import com.krushna.moviebooking.auth.dto.response.UserResponse;
import com.krushna.moviebooking.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, login, token refresh, and validation")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure or email already registered")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("REST POST /api/v1/auth/register email='{}'", request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Authenticate user and issue JWT tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("REST POST /api/v1/auth/login email='{}'", request.email());
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Renew access token using refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @Operation(summary = "Validate access token (used by API Gateway)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token validation result returned",
                    content = @Content(schema = @Schema(implementation = TokenValidationResponse.class)))
    })
    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(@Valid @RequestBody ValidateTokenRequest request) {
        return ResponseEntity.ok(authService.validateToken(request.token()));
    }

    @Operation(summary = "Get user details by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile details returned",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/user/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "User UUID", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(authService.getUserById(id));
    }

    @Operation(summary = "Logout user (revoke refresh tokens)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User logged out successfully and tokens revoked")
    })
    @PostMapping("/logout/{userId}")
    public ResponseEntity<Void> logout(
            @Parameter(description = "User UUID", required = true) @PathVariable UUID userId) {
        authService.logout(userId);
        return ResponseEntity.noContent().build();
    }
}

