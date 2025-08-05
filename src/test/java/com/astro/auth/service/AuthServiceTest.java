package com.astro.auth.service;

import com.astro.auth.dto.RegisterRequest;
import com.astro.user.model.User;
import com.astro.user.plan.model.Plan;
import com.astro.user.plan.repository.PlanRepository;
import com.astro.user.repository.UserRepository;
import com.astro.shared.exceptions.UserAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PlanRepository planRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private Plan polarisPlan;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@email.com");
        registerRequest.setPassword("password123");
        registerRequest.setConfirmPassword("password123");

        polarisPlan = new Plan();
        polarisPlan.setName("Polaris");
    }

    @Test
    void registerUser_shouldSucceed_whenDataIsVal() {
        // Given
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(planRepository.findByName("Polaris")).thenReturn(Optional.of(polarisPlan));
        when(passwordEncoder.encode(anyString())).thenReturn("hashedpassword");

        // When
        authService.registerUser(registerRequest);

        // Then
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void registerUser_shouldFail_whenPasswordsDoNotMatch() {
        // Given
        registerRequest.setConfirmPassword("wrongpassword");

        // Then
        assertThatThrownBy(() -> authService.registerUser(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Passwords do not match.");
    }

    @Test
    void registerUser_shouldFail_whenUsernameIsTaken() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(new User()));

        // Then
        assertThatThrownBy(() -> authService.registerUser(registerRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("Username 'testuser' is already taken.");
    }
}