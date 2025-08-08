package com.astro.url.controller;

import com.astro.config.BaseControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UrlManagementControllerTest extends BaseControllerTest {

    @Test
    void getUserUrls_shouldReturnPagedListOfUrls() throws Exception {
        String token = getAccessToken("list-user", "list@test.com", "password123");
        var user = userRepository.findByIdentifierWithPlan("list-user").orElseThrow();
        for (int i = 0; i < 6; i++) {
            createTestUrl("https://example.com/" + i, "slug" + i, user);
        }

        mockMvc.perform(get("/api/url?page=0&size=5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(5)))
                .andExpect(jsonPath("$.totalPages", is(2)));
    }

    @Test
    void isSlugAvailable_shouldReturnTrue_forAvailableSlug() throws Exception {
        String token = getAccessToken("any-user", "any@test.com", "password123");

        mockMvc.perform(get("/api/url/validate-slug/available-slug")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available", is(true)));
    }
}