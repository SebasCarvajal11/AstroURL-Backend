package com.astro.config;

import com.astro.auth.dto.LoginRequest;
import com.astro.auth.dto.LoginResponse;
import com.astro.stats.repository.ClickRepository;
import com.astro.url.model.Url;
import com.astro.url.repository.UrlRepository;
import com.astro.user.UserBuilder; // Importar el UserBuilder
import com.astro.user.model.User;
import com.astro.user.plan.model.Plan;
import com.astro.user.plan.repository.PlanRepository;
import com.astro.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseControllerTest extends AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected PlanRepository planRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected RedisTemplate<String, String> redisTemplate;
    @Autowired
    protected UrlRepository urlRepository;
    @Autowired
    protected ClickRepository clickRepository;

    @MockBean
    protected Clock clock;

    protected static final Instant MOCK_TIME_NOW = Instant.parse("2025-08-10T10:00:00Z");

    @BeforeEach
    void baseSetUp() {
        clickRepository.deleteAll();
        urlRepository.deleteAll();
        userRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushDb();

        when(clock.instant()).thenReturn(MOCK_TIME_NOW);
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
    }

    /**
     * CORRECCIÓN: El método ahora utiliza el UserBuilder para crear la instancia de User,
     * haciendo el código más limpio y mantenible.
     */
    protected User createTestUser(String username, String email, String rawPassword, String planName) {
        Plan plan = planRepository.findByName(planName)
                .orElseThrow(() -> new IllegalStateException("Plan not found: " + planName));

        User user = new UserBuilder()
                .withUsername(username)
                .withEmail(email)
                .withPassword(rawPassword)
                .withPlan(plan)
                .build(passwordEncoder);

        return userRepository.save(user);
    }

    protected Url createTestUrl(String originalUrl, String slug, User user) {
        Url url = Url.createAuthenticatedUrl(
                originalUrl,
                slug,
                user,
                LocalDateTime.now(clock).plusDays(10),
                null,
                passwordEncoder::encode
        );
        return urlRepository.save(url);
    }

    protected LoginResponse getLoginResponse(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginIdentifier(username);
        loginRequest.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), LoginResponse.class);
    }

    protected String getAccessToken(String username, String email, String rawPassword, String planName) throws Exception {
        createTestUser(username, email, rawPassword, planName);
        return getLoginResponse(username, rawPassword).getAccessToken();
    }
}