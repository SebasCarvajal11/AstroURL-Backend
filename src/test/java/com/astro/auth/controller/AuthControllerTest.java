package com.astro.auth.controller;

import com.astro.config.AbstractIntegrationTest;
import com.astro.auth.dto.*;
import com.astro.shared.service.EmailService;
import com.astro.user.model.User;
import com.astro.user.plan.model.Plan;
import com.astro.user.repository.UserRepository;
import com.astro.user.plan.repository.PlanRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.notNullValue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

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
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @MockBean
    private Clock clock;

    @SpyBean
    private EmailService emailService;

    private static final Instant MOCK_TIME_NOW = Instant.parse("2025-08-10T10:00:00Z");
    private static final Instant MOCK_TIME_LATER = Instant.parse("2025-08-10T10:00:10Z");

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushDb();
        when(clock.instant()).thenReturn(MOCK_TIME_NOW);
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
    }

    @Test
    void refreshToken_shouldSucceed_whenTokenIsValid() throws Exception {
        when(clock.instant()).thenReturn(MOCK_TIME_NOW);
        createTestUser("refresh-user", "refresh@test.com", "password123");
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier("refresh-user");
        loginRequest.setPassword("password123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();
        LoginResponse loginResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), LoginResponse.class);
        String originalRefreshToken = loginResponse.getRefreshToken();
        String originalAccessToken = loginResponse.getAccessToken();

        when(clock.instant()).thenReturn(MOCK_TIME_LATER);
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(originalRefreshToken);
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andReturn();

        LoginResponse refreshResponse = objectMapper.readValue(refreshResult.getResponse().getContentAsString(), LoginResponse.class);
        assertThat(refreshResponse.getAccessToken()).isNotEqualTo(originalAccessToken);
        assertThat(refreshResponse.getRefreshToken()).isNotEqualTo(originalRefreshToken);
    }

    @Test
    void refreshToken_shouldFail_whenTokenIsInvalid() throws Exception {
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken("this.is.a.bogus.token");

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    void forgotPassword_shouldReturnOkAndSendEmail_whenUserExists() throws Exception {
        createTestUser("forgot-user", "forgot@test.com", "password123");
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("forgot@test.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailService, timeout(1000).times(1)).sendPasswordResetEmail(
                userCaptor.capture(), tokenCaptor.capture(), anyString()
        );

        assertThat(userCaptor.getValue().getEmail()).isEqualTo("forgot@test.com");
        String token = tokenCaptor.getValue();
        assertThat(redisTemplate.hasKey("user:resetToken:" + token)).isTrue();
    }

    @Test
    void forgotPassword_shouldReturnOkAndNotSendEmail_whenUserDoesNotExist() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("nonexistent@test.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(emailService, never()).sendPasswordResetEmail(any(User.class), anyString(), anyString());
    }

    @Test
    void resetPassword_shouldSucceed_whenTokenIsValid() throws Exception {
        User user = createTestUser("reset-user", "reset@test.com", "oldPassword123");
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("user:resetToken:" + token, user.getId().toString(), 15, TimeUnit.MINUTES);

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(token);
        request.setNewPassword("newStrongPassword");
        request.setConfirmPassword("newStrongPassword");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("newStrongPassword", updatedUser.getPassword())).isTrue();
        assertThat(redisTemplate.hasKey("user:resetToken:" + token)).isFalse();
    }

    @Test
    void resetPassword_shouldFail_whenTokenIsInvalid() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("invalid-token");
        request.setNewPassword("newStrongPassword");
        request.setConfirmPassword("newStrongPassword");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_shouldFail_whenPasswordsDoNotMatch() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("any-token");
        request.setNewPassword("newStrongPassword");
        request.setConfirmPassword("non-matching-password");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Passwords do not match.")));
    }

    @Test
    void logoutAll_shouldSucceedAndRevokeTokens_whenPasswordIsCorrect() throws Exception {
        String token = getAuthToken("logout-user", "logout@test.com", "password123");
        User user = userRepository.findByIdentifierWithPlan("logout-user").orElseThrow();
        String refreshTokenKey = "user:refreshToken:" + user.getId();

        assertThat(redisTemplate.hasKey(refreshTokenKey)).isTrue();

        PasswordConfirmationRequest request = new PasswordConfirmationRequest();
        request.setCurrentPassword("password123");

        mockMvc.perform(post("/api/auth/logout-all")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        assertThat(redisTemplate.hasKey(refreshTokenKey)).isFalse();
        assertThat(redisTemplate.hasKey("user:tokenBlacklist:" + user.getId())).isTrue();

        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutAll_shouldFail_whenPasswordIsIncorrect() throws Exception {
        String token = getAuthToken("logout-fail-user", "logoutfail@test.com", "password123");
        PasswordConfirmationRequest request = new PasswordConfirmationRequest();
        request.setCurrentPassword("wrongPassword");

        mockMvc.perform(post("/api/auth/logout-all")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private String getAuthToken(String username, String email, String rawPassword) throws Exception {
        createTestUser(username, email, rawPassword);
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier(username);
        loginRequest.setPassword(rawPassword);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(result.getResponse().getContentAsString(), LoginResponse.class);
        return loginResponse.getAccessToken();
    }

    private User createTestUser(String username, String email, String rawPassword) {
        Plan polarisPlan = planRepository.findByName("Polaris").orElseThrow();
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPlan(polarisPlan);
        return userRepository.save(user);
    }
}