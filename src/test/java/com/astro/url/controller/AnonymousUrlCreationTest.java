package com.astro.url.controller;

import com.astro.config.BaseControllerTest;
import com.astro.url.dto.UrlShortenRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasLength;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AnonymousUrlCreationTest extends BaseControllerTest {

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
    void shortenUrl_shouldFail_whenAnonymousRateLimitIsExceeded() throws Exception {
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
                .andExpect(status().isTooManyRequests());
    }
}