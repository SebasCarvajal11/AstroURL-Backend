package com.astro.auth.controller;

import com.astro.config.AbstractIntegrationTest;
import com.astro.auth.dto.ForgotPasswordRequest;
import com.astro.auth.dto.LoginRequest;
import com.astro.auth.dto.LoginResponse;
import com.astro.auth.dto.RefreshTokenRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.is;

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