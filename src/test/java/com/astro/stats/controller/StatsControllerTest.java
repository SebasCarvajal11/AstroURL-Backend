package com.astro.stats.controller;

import com.astro.config.BaseControllerTest;
import com.astro.stats.model.Click;
import com.astro.stats.repository.ClickRepository;
import com.astro.url.model.Url;
import com.astro.url.repository.UrlRepository;
import com.astro.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class StatsControllerTest extends BaseControllerTest {

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private ClickRepository clickRepository;

    @Test
    void getUrlStats_shouldSucceed_whenUserOwnsTheSlug() throws Exception {
        String token = getAccessToken("stats-user", "stats@test.com", "password123");
        User user = userRepository.findByIdentifierWithPlan("stats-user").orElseThrow();
        Url url = createTestUrl("https://stats.com", "stats-slug", user, 5);

        Click click = new Click();
        click.setUrl(url);
        click.setIpAddress("1.1.1.1");
        clickRepository.save(click);

        mockMvc.perform(get("/api/stats/stats-slug")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug", is("stats-slug")))
                .andExpect(jsonPath("$.totalClicks", is(5)))
                .andExpect(jsonPath("$.lastAccessed").exists());
    }

    @Test
    void getUrlStats_shouldFailWithForbidden_whenUserDoesNotOwnTheSlug() throws Exception {
        User owner = createTestUser("owner", "owner@test.com", "password123");
        createTestUrl("https://secret.com", "secret-slug", owner, 0);

        String attackerToken = getAccessToken("attacker", "attacker@test.com", "password123");

        mockMvc.perform(get("/api/stats/secret-slug")
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isForbidden());
    }

    private Url createTestUrl(String originalUrl, String slug, User user, long clickCount) {
        Url url = new Url();
        url.setOriginalUrl(originalUrl);
        url.setSlug(slug);
        url.setUser(user);
        url.setExpirationDate(LocalDateTime.now().plusDays(10));
        url.setClickCount(clickCount);
        return urlRepository.save(url);
    }
}