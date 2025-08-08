package com.astro.url.controller;

import com.astro.config.BaseControllerTest;
import com.astro.url.dto.UrlUpdateRequest;
import com.astro.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UrlManagementControllerTest extends BaseControllerTest {

    @Test
    void getUserUrls_shouldReturnPagedListOfUrls() throws Exception {
        String token = getAccessToken("list-user", "list@test.com", "password123", "Polaris");
        User user = userRepository.findByIdentifierWithPlan("list-user").orElseThrow();
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
    void isSlugAvailable_shouldReturnTrue_forAvailableSlugBySirioUser() throws Exception {
        String token = getAccessToken("sirio-slug-check", "sirio-check@test.com", "password123", "Sirio");

        mockMvc.perform(get("/api/url/validate-slug/available-slug")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available", is(true)));
    }

    @Test
    void isSlugAvailable_shouldReturnFalse_forTakenSlugBySirioUser() throws Exception {
        createTestUser("some-user", "some@test.com", "password123", "Polaris");
        User someUser = userRepository.findByIdentifierWithPlan("some-user").orElseThrow();
        createTestUrl("https://existing.com", "taken-slug", someUser);

        String token = getAccessToken("sirio-slug-check-2", "sirio-check-2@test.com", "password123", "Sirio");

        mockMvc.perform(get("/api/url/validate-slug/taken-slug")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available", is(false)));
    }

    @Test
    void isSlugAvailable_shouldFailWithForbidden_forPolarisUser() throws Exception {
        String token = getAccessToken("polaris-slug-check", "polaris-check@test.com", "password123", "Polaris");

        mockMvc.perform(get("/api/url/validate-slug/any-slug")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUrl_shouldSucceed_forSirioUserAndOwner() throws Exception {
        String token = getAccessToken("sirio-owner", "sirio-owner@test.com", "password123", "Sirio");
        User user = userRepository.findByIdentifierWithPlan("sirio-owner").orElseThrow();
        var url = createTestUrl("https://original.com", "originalslug", user);

        UrlUpdateRequest request = new UrlUpdateRequest();
        request.setSlug("updatedslug");

        mockMvc.perform(put("/api/url/" + url.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug", is("updatedslug")));
    }

    @Test
    void updateUrl_shouldFailWithForbidden_forPolarisUser() throws Exception {
        String token = getAccessToken("polaris-updater", "polaris-up@test.com", "password123", "Polaris");
        User user = userRepository.findByIdentifierWithPlan("polaris-updater").orElseThrow();
        var url = createTestUrl("https://polaris.com", "polarisslug", user);

        UrlUpdateRequest request = new UrlUpdateRequest();
        request.setSlug("newslug");

        mockMvc.perform(put("/api/url/" + url.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}