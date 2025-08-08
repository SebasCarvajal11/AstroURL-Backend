package com.astro.url.controller;

import com.astro.config.BaseControllerTest;
import com.astro.url.dto.UrlShortenRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.startsWith;
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

    @Test
    void shortenUrl_shouldSucceedWithCustomExpiration_forSirioUser() throws Exception {
        String token = getAccessToken("sirio-exp-user", "sirio-exp@test.com", "password123", "Sirio");
        UrlShortenRequest request = new UrlShortenRequest();
        request.setOriginalUrl("https://premium-link.com");

        LocalDateTime futureDate = LocalDateTime.now(clock).plusDays(100);
        request.setExpirationDate(futureDate);

        mockMvc.perform(post("/api/url")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expirationDate", startsWith(futureDate.toString())));
    }

    @Test
    void shortenUrl_shouldFail_whenPolarisUserTriesCustomExpiration() throws Exception {
        String token = getAccessToken("polaris-exp-user", "polaris-exp@test.com", "password123", "Polaris");
        UrlShortenRequest request = new UrlShortenRequest();
        request.setOriginalUrl("https://forbidden-link.com");
        request.setExpirationDate(LocalDateTime.now(clock).plusDays(20));

        mockMvc.perform(post("/api/url")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}