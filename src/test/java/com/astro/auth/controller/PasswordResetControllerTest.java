package com.astro.auth.controller;

import com.astro.auth.dto.ForgotPasswordRequest;
import com.astro.auth.dto.ResetPasswordRequest;
import com.astro.config.BaseControllerTest;
import com.astro.shared.service.EmailService;
import com.astro.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PasswordResetControllerTest extends BaseControllerTest {

    @SpyBean
    private EmailService emailService;

    @Test
    void forgotPassword_shouldSendEmail_whenUserExists() throws Exception {
        createTestUser("forgot-user", "forgot@test.com", "password123", "Polaris");
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("forgot@test.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(emailService, timeout(1000).times(1)).sendPasswordResetEmail(any(User.class), anyString(), anyString());
    }

    @Test
    void resetPassword_shouldSucceed_whenTokenIsValid() throws Exception {
        User user = createTestUser("reset-user", "reset@test.com", "oldPassword", "Polaris");
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("user:resetToken:" + token, user.getId().toString(), 15, TimeUnit.MINUTES);

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(token);
        request.setNewPassword("newPassword");
        request.setConfirmPassword("newPassword");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("newPassword", updatedUser.getPassword())).isTrue();
    }
}