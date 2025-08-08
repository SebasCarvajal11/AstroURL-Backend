package com.astro.auth.controller;

import com.astro.auth.dto.LoginResponse;
import com.astro.auth.dto.PasswordConfirmationRequest;
import com.astro.auth.dto.RefreshTokenRequest;
import com.astro.config.BaseControllerTest;
import com.astro.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TokenControllerTest extends BaseControllerTest {

    private static final Instant MOCK_TIME_LATER = Instant.parse("2025-08-10T10:00:10Z");

    @Test
    void refreshToken_shouldSucceed_whenTokenIsValid() throws Exception {
        createTestUser("refresh-user", "refresh@test.com", "password123", "Polaris");
        LoginResponse loginResponse = getLoginResponse("refresh-user", "password123");

        when(clock.instant()).thenReturn(MOCK_TIME_LATER);
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(loginResponse.getRefreshToken());

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse refreshResponse = objectMapper.readValue(refreshResult.getResponse().getContentAsString(), LoginResponse.class);
        assertThat(refreshResponse.getAccessToken()).isNotEqualTo(loginResponse.getAccessToken());
    }

    @Test
    void refreshToken_shouldFail_whenTokenIsInvalid() throws Exception {
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken("this.is.a.bogus.token");

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutAll_shouldSucceedAndRevokeTokens() throws Exception {
        String token = getAccessToken("logout-user", "logout@test.com", "password123", "Polaris");
        User user = userRepository.findByIdentifierWithPlan("logout-user").orElseThrow();

        PasswordConfirmationRequest request = new PasswordConfirmationRequest();
        request.setCurrentPassword("password123");

        mockMvc.perform(post("/api/auth/logout-all")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}