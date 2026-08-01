package com.krushna.moviebooking.auth.service;

import com.krushna.moviebooking.auth.dto.request.LoginRequest;
import com.krushna.moviebooking.auth.dto.request.RefreshTokenRequest;
import com.krushna.moviebooking.auth.dto.request.RegisterRequest;
import com.krushna.moviebooking.auth.dto.response.AuthResponse;
import com.krushna.moviebooking.auth.dto.response.TokenValidationResponse;
import com.krushna.moviebooking.auth.dto.response.UserResponse;

import java.util.UUID;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    TokenValidationResponse validateToken(String token);

    UserResponse getUserById(UUID id);

    void logout(UUID userId);
}
