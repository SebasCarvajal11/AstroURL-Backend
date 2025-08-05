package com.astro.url.controller;

import com.astro.config.AbstractIntegrationTest;
import com.astro.url.dto.UrlShortenRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }

    @Test
    void shortenUrl_shouldSucceed_forValidAnonymousRequest() throws Exception {
        UrlShortenRequest request = new UrlShortenRequest();
        request.setOriginalUrl("https://google.com");

        mockMvc.perform(post("/api/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").isString())
                .andExpect(jsonPath("$.slug").isNotEmpty())
                .andExpect(jsonPath("$.shortUrl").isString());
    }

    @Test
    void shortenUrl_shouldFail_whenUrlIsInvalid() throws Exception {
        UrlShortenRequest request = new UrlShortenRequest();
        request.setOriginalUrl("not-a-valid-url");

        mockMvc.perform(post("/api/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shortenUrl_shouldFail_whenRateLimitIsExceeded() throws Exception {
        UrlShortenRequest request = new UrlShortenRequest();
        request.setOriginalUrl("https://example.com");

        // Perform 7 successful requests
        for (int i = 0; i < 7; i++) {
            mockMvc.perform(post("/api/url")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // The 8th request should fail
        mockMvc.perform(post("/api/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Rate limit of 7 URLs per day exceeded for your IP address."));
    }
}