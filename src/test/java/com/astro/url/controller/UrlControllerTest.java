package com.astro.url.controller;

import com.astro.config.AbstractIntegrationTest;
import com.astro.auth.dto.LoginRequest;
import com.astro.auth.dto.LoginResponse;
import com.astro.url.dto.UrlShortenRequest;
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

import static org.hamcrest.Matchers.hasLength;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

        // Clean dependent entities first (Url) before the parent (User)
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