package com.astro.auth.controller;

import com.astro.config.AbstractIntegrationTest;
import com.astro.auth.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerUser_shouldReturnCreated_whenRequestIsValid() throws Exception {
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
    }

    @Test
    void registerUser_shouldReturnConflict_whenUsernameExists() throws Exception {
        // First, register a user
        RegisterRequest request1 = new RegisterRequest();
        request1.setUsername("existing-user");
        request1.setEmail("user1@astrourl.com");
        request1.setPassword("password123");
        request1.setConfirmPassword("password123");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)));

        // Then, try to register with the same username
        RegisterRequest request2 = new RegisterRequest();
        request2.setUsername("existing-user");
        request2.setEmail("user2@astrourl.com");
        request2.setPassword("password123");
        request2.setConfirmPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Username 'existing-user' is already taken."));
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenPasswordIsTooShort() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("short-pass-user");
        request.setEmail("shortpass@astrourl.com");
        request.setPassword("123");
        request.setConfirmPassword("123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors.password").value("Password must be at least 7 characters long"));
    }
}