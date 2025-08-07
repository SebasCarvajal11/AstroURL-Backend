package com.astro.auth.controller;

import com.astro.auth.dto.RegisterRequest;
import com.astro.config.BaseControllerTest;
import com.astro.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RegistrationControllerTest extends BaseControllerTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void registerUser_shouldSucceed_whenDataIsValid() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("new-user");
        request.setEmail("newuser@astrourl.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully!"));

        assertThat(userRepository.findByUsername("new-user")).isPresent();
    }

    @Test
    void registerUser_shouldFail_whenUsernameIsTaken() throws Exception {
        createTestUser("existing-user", "user1@astrourl.com", "password123");

        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing-user");
        request.setEmail("user2@astrourl.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message").value("Username 'existing-user' is already taken."));
    }

    @Test
    void registerUser_shouldFail_whenPasswordIsTooShort() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("short-pass-user");
        request.setEmail("shortpass@astrourl.com");
        request.setPassword("123");
        request.setConfirmPassword("123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.subErrors.password").value("Password must be at least 7 characters long"));
    }
}