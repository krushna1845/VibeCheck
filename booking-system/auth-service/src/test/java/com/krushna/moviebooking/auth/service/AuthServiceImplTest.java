package com.krushna.moviebooking.auth.service;

import com.krushna.moviebooking.auth.dto.request.LoginRequest;
import com.krushna.moviebooking.auth.dto.request.RegisterRequest;
import com.krushna.moviebooking.auth.dto.response.AuthResponse;
import com.krushna.moviebooking.auth.dto.response.UserResponse;
import com.krushna.moviebooking.auth.entity.Role;
import com.krushna.moviebooking.auth.entity.User;
import com.krushna.moviebooking.auth.exception.EmailAlreadyExistsException;
import com.krushna.moviebooking.auth.exception.InvalidCredentialsException;
import com.krushna.moviebooking.auth.mapper.UserMapper;
import com.krushna.moviebooking.auth.repository.RefreshTokenRepository;
import com.krushna.moviebooking.auth.repository.RoleRepository;
import com.krushna.moviebooking.auth.repository.UserRepository;
import com.krushna.moviebooking.auth.security.JwtTokenProvider;
import com.krushna.moviebooking.auth.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private UUID userId;
    private User user;
    private Role userRole;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userRole = Role.builder().id(1).name("ROLE_USER").build();

        user = User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .firstName("Test")
                .lastName("User")
                .phoneNumber("9876543210")
                .status("ACTIVE")
                .roles(Set.of(userRole))
                .createdAt(Instant.now())
                .build();

        userResponse = UserResponse.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .phoneNumber("9876543210")
                .status("ACTIVE")
                .roles(Set.of("ROLE_USER"))
                .build();
    }

    @Test
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .phoneNumber("9876543210")
                .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("9876543210")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenProvider.generateAccessToken(eq(userId), eq("test@example.com"), any())).thenReturn("access.jwt");
        when(tokenProvider.generateRefreshToken(eq(userId))).thenReturn("refresh.jwt");
        when(tokenProvider.getAccessTokenExpirationMs()).thenReturn(86400000L);
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access.jwt");
        assertThat(response.user().email()).isEqualTo("test@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_EmailAlreadyExists_ThrowsException() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .phoneNumber("9876543210")
                .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(tokenProvider.generateAccessToken(eq(userId), eq("test@example.com"), any())).thenReturn("access.jwt");
        when(tokenProvider.generateRefreshToken(eq(userId))).thenReturn("refresh.jwt");
        when(tokenProvider.getAccessTokenExpirationMs()).thenReturn(86400000L);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access.jwt");
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
