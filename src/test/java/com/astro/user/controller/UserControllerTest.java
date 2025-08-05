package com.astro.user.controller;

import com.astro.config.AbstractIntegrationTest;
import com.astro.auth.dto.LoginRequest;
import com.astro.auth.dto.LoginResponse;
import com.astro.user.model.User;
import com.astro.user.plan.model.Plan;
import com.astro.user.plan.repository.PlanRepository;
import com.astro.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest extends AbstractIntegrationTest {

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
        userRepository.deleteAll();
    }

    @Test
    void getUserProfile_shouldSucceed_whenUserIsAuthenticated() throws Exception {
        // Given a logged-in user
        String token = getAuthToken("profile-user", "profile@test.com", "password123");

        // When
        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + token))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("profile-user"))
                .andExpect(jsonPath("$.email").value("profile@test.com"))
                .andExpect(jsonPath("$.planName").value("Polaris"));
    }

    @Test
    void getUserProfile_shouldFailWithUnauthorized_whenNoTokenIsProvided() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());
    }

    private String getAuthToken(String username, String email, String rawPassword) throws Exception {
        createTestUser(username, email, rawPassword);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier(username);
        loginRequest.setPassword(rawPassword);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        LoginResponse loginResponse = objectMapper.readValue(response, LoginResponse.class);
        return loginResponse.getAccessToken();
    }

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