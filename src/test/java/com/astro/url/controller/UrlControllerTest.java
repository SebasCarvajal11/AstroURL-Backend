package com.astro.url.controller;

import com.astro.config.AbstractIntegrationTest;
import com.astro.auth.dto.LoginRequest;
import com.astro.auth.dto.LoginResponse;
import com.astro.url.dto.UrlShortenRequest;
import com.astro.url.model.Url;
import com.astro.url.repository.UrlRepository;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UrlControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlanRepository planRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UrlRepository urlRepository;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().flushDb();
        urlRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shortenUrl_shouldSucceed_forValidAnonymousRequest() throws Exception {
        UrlShortenRequest request = new UrlShortenRequest();
        request.setOriginalUrl("https://google.com");

        mockMvc.perform(post("/api/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug", hasLength(6)))
                .andExpect(jsonPath("$.shortUrl").isString());
    }

    @Test
    void shortenUrl_shouldSucceed_forAuthenticatedUser() throws Exception {
        String token = getAuthToken("polaris-user", "polaris@test.com", "password123");

        UrlShortenRequest request = new UrlShortenRequest();
        request.setOriginalUrl("https://github.com");

        mockMvc.perform(post("/api/url")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug", hasLength(5)));
    }

    @Test
    void shortenUrl_shouldFail_forAuthenticatedUser_whenRateLimitIsExceeded() throws Exception {
        String token = getAuthToken("rate-limit-user", "rate@test.com", "password123");
        UrlShortenRequest request = new UrlShortenRequest();
        request.setOriginalUrl("https://example.com");

        for (int i = 0; i < 15; i++) {
            mockMvc.perform(post("/api/url")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/url")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Your daily URL creation limit has been reached."));
    }

    @Test
    void getUserUrls_shouldReturnPagedListOfUrls_forAuthenticatedUser() throws Exception {
        // Given a logged-in user with 6 URLs
        String token = getAuthToken("list-user", "list@test.com", "password123");
        User user = userRepository.findByIdentifierWithPlan("list-user").orElseThrow();
        for (int i = 0; i < 6; i++) {
            createTestUrl("https://example.com/" + i, "slug" + i, user);
        }

        // When requesting the first page
        mockMvc.perform(get("/api/url?page=0&size=5")
                        .header("Authorization", "Bearer " + token))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(5)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.totalElements", is(6)))
                .andExpect(jsonPath("$.number", is(0)));

        // When requesting the second page
        mockMvc.perform(get("/api/url?page=1&size=5")
                        .header("Authorization", "Bearer " + token))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)));
    }

    @Test
    void getUserUrls_shouldFailWithUnauthorized_whenNoTokenIsProvided() throws Exception {
        mockMvc.perform(get("/api/url"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteUrl_shouldSucceed_whenUserOwnsTheUrl() throws Exception {
        // Given
        String token = getAuthToken("delete-user", "delete@test.com", "password123");
        User user = userRepository.findByIdentifierWithPlan("delete-user").orElseThrow();
        Url url = createTestUrl("https://todelete.com", "deleteme", user);

        // When & Then
        mockMvc.perform(delete("/api/url/" + url.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(urlRepository.findById(url.getId())).isEmpty();
    }

    @Test
    void deleteUrl_shouldFailWithForbidden_whenUserDoesNotOwnTheUrl() throws Exception {
        // Given
        User owner = createTestUser("owner", "owner@test.com", "password123");
        Url url = createTestUrl("https://secret.com", "secret", owner);

        String attackerToken = getAuthToken("attacker", "attacker@test.com", "password123");

        // When & Then
        mockMvc.perform(delete("/api/url/" + url.getId())
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isForbidden());

        assertThat(urlRepository.findById(url.getId())).isPresent();
    }

    @Test
    void deleteUrl_shouldFailWithNotFound_whenUrlDoesNotExist() throws Exception {
        // Given
        String token = getAuthToken("any-user", "any@test.com", "password123");
        long nonExistentId = 999L;

        // When & Then
        mockMvc.perform(delete("/api/url/" + nonExistentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shortenUrl_shouldFail_whenRateLimitIsExceeded() throws Exception {
        UrlShortenRequest request = new UrlShortenRequest();
        request.setOriginalUrl("https://example.com");

        for (int i = 0; i < 7; i++) {
            mockMvc.perform(post("/api/url")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status", is(429)))
                .andExpect(jsonPath("$.error", is("Too Many Requests")))
                .andExpect(jsonPath("$.message", is("Rate limit of 7 URLs per day exceeded for your IP address.")));
    }

    private String getAuthToken(String username, String email, String rawPassword) throws Exception {
        createTestUser(username, email, rawPassword);
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier(username);
        loginRequest.setPassword(rawPassword);
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(response, LoginResponse.class);
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

    private Url createTestUrl(String originalUrl, String slug, User user) {
        Url url = new Url();
        url.setOriginalUrl(originalUrl);
        url.setSlug(slug);
        url.setUser(user);
        url.setExpirationDate(LocalDateTime.now().plusDays(10));
        return urlRepository.save(url);
    }
}