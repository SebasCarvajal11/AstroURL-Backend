package com.astro.auth.controller;

import com.astro.config.AbstractIntegrationTest;
import com.astro.auth.dto.LoginRequest;
import com.astro.auth.dto.RegisterRequest;
import com.astro.user.model.User;
import com.astro.user.plan.model.Plan;
import com.astro.user.repository.UserRepository;
import com.astro.user.plan.repository.PlanRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlanRepository planRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Clean up users before each test to ensure isolation
        userRepository.deleteAll();
    }

    @Test
    void loginUser_shouldReturnTokens_whenCredentialsAreValidWithUsername() throws Exception {
        // Given: a registered user
        createTestUser("login-user", "login@test.com", "password123");

        // When
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier("login-user");
        loginRequest.setPassword("password123");

        // Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()));
    }

    @Test
    void loginUser_shouldReturnTokens_whenCredentialsAreValidWithEmail() throws Exception {
        // Given: a registered user
        createTestUser("login-user-2", "login2@test.com", "password123");

        // When
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier("login2@test.com");
        loginRequest.setPassword("password123");

        // Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()));
    }

    @Test
    void loginUser_shouldReturnUnauthorized_whenPasswordIsIncorrect() throws Exception {
        // Given: a registered user
        createTestUser("login-user-3", "login3@test.com", "password123");

        // When
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier("login-user-3");
        loginRequest.setPassword("wrong-password");

        // Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    // Helper method to create a user for tests
    private void createTestUser(String username, String email, String rawPassword) {
        Plan polarisPlan = planRepository.findByName("Polaris").orElseThrow();
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPlan(polarisPlan);
        userRepository.save(user);
    }
}