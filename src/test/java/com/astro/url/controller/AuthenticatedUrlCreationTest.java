package com.astro.url.controller;

import com.astro.config.BaseControllerTest;
import com.astro.url.dto.UrlShortenRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasLength;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthenticatedUrlCreationTest extends BaseControllerTest {

    @Test
    void shortenUrl_shouldSucceed_forAuthenticatedUser() throws Exception {
        String token = getAccessToken("polaris-user", "polaris@test.com", "password123", "Polaris");

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
        String token = getAccessToken("rate-limit-user", "rate@test.com", "password123", "Polaris");
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
                .andExpect(status().isTooManyRequests());
    }
}