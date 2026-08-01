package com.krushna.moviebooking.auth.service.impl;

import com.krushna.moviebooking.auth.dto.request.LoginRequest;
import com.krushna.moviebooking.auth.dto.request.RefreshTokenRequest;
import com.krushna.moviebooking.auth.dto.request.RegisterRequest;
import com.krushna.moviebooking.auth.dto.response.AuthResponse;
import com.krushna.moviebooking.auth.dto.response.TokenValidationResponse;
import com.krushna.moviebooking.auth.dto.response.UserResponse;
import com.krushna.moviebooking.auth.entity.RefreshToken;
import com.krushna.moviebooking.auth.entity.Role;
import com.krushna.moviebooking.auth.entity.User;
import com.krushna.moviebooking.auth.exception.*;
import com.krushna.moviebooking.auth.mapper.UserMapper;
import com.krushna.moviebooking.auth.repository.RefreshTokenRepository;
import com.krushna.moviebooking.auth.repository.RoleRepository;
import com.krushna.moviebooking.auth.repository.UserRepository;
import com.krushna.moviebooking.auth.security.JwtTokenProvider;
import com.krushna.moviebooking.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: '{}'", request.email());

        if (userRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        if (userRepository.existsByPhoneNumber(request.phoneNumber().trim())) {
            throw new PhoneNumberAlreadyExistsException(request.phoneNumber());
        }

        Set<Role> roles = resolveRoles(request.roles());

        User user = User.builder()
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .phoneNumber(request.phoneNumber().trim())
                .status("ACTIVE")
                .isEmailVerified(true)
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with id: {}", savedUser.getId());

        return createAuthResponse(savedUser);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("User login attempt for email: '{}'", request.email());

        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (user.getDeletedAt() != null || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new InvalidCredentialsException("User account is inactive or suspended");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        log.info("User logged in successfully: id={}", user.getId());
        return createAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tokenStr = request.refreshToken();

        if (!tokenProvider.validateToken(tokenStr)) {
            throw new InvalidTokenException("Refresh token is invalid or expired");
        }

        String hash = hashToken(tokenStr);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (refreshToken.getIsRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        User user = refreshToken.getUser();
        List<String> roleNames = user.getRoles().stream().map(Role::getName).toList();
        String newAccessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail(), roleNames);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(tokenStr)
                .tokenType("Bearer")
                .expiresInMs(tokenProvider.getAccessTokenExpirationMs())
                .user(userMapper.toResponse(user))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TokenValidationResponse validateToken(String token) {
        if (!tokenProvider.validateToken(token)) {
            return TokenValidationResponse.builder().valid(false).build();
        }

        String userIdStr = tokenProvider.getUserIdFromToken(token);
        String email = tokenProvider.getEmailFromToken(token);
        List<String> roles = tokenProvider.getRolesFromToken(token);

        return TokenValidationResponse.builder()
                .valid(true)
                .userId(UUID.fromString(userIdStr))
                .email(email)
                .roles(roles)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void logout(UUID userId) {
        log.info("Revoking refresh tokens for user id: {}", userId);
        refreshTokenRepository.revokeAllUserTokens(userId);
    }

    private Set<Role> resolveRoles(Set<String> requestedRoles) {
        Set<Role> roles = new HashSet<>();
        if (requestedRoles == null || requestedRoles.isEmpty()) {
            roleRepository.findByName("ROLE_USER").ifPresent(roles::add);
        } else {
            for (String roleName : requestedRoles) {
                String formatted = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
                Role role = roleRepository.findByName(formatted)
                        .orElseGet(() -> roleRepository.save(Role.builder().name(formatted).build()));
                roles.add(role);
            }
        }
        return roles;
    }

    private AuthResponse createAuthResponse(User user) {
        List<String> roleNames = user.getRoles().stream().map(Role::getName).toList();
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail(), roleNames);
        String refreshTokenStr = tokenProvider.generateRefreshToken(user.getId());

        String hash = hashToken(refreshTokenStr);
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .tokenHash(hash)
                .isRevoked(false)
                .expiresAt(Instant.now().plusMillis(604800000)) // 7 days
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .expiresInMs(tokenProvider.getAccessTokenExpirationMs())
                .user(userMapper.toResponse(user))
                .build();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
