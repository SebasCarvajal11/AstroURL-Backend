package com.astro.stats.controller;

import com.astro.config.BaseControllerTest;
import com.astro.stats.model.Click;
import com.astro.user.model.User;
import com.astro.url.model.Url;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class StatsControllerTest extends BaseControllerTest {

    @Test
    void getUrlStats_shouldReturnFullStatsIncluding7DayChart() throws Exception {
        String token = getAccessToken("stats-user", "stats@test.com", "password123", "Polaris");
        User user = userRepository.findByIdentifierWithPlan("stats-user").orElseThrow();
        Url url = createTestUrl("https://stats.com", "stats-slug", user);

        LocalDateTime today = LocalDateTime.now(clock);
        LocalDateTime twoDaysAgo = today.minusDays(2);

        createClick(url, today);
        createClick(url, today);
        createClick(url, twoDaysAgo);

        url.setClickCount(3);
        urlRepository.save(url);

        mockMvc.perform(get("/api/stats/stats-slug")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug", is("stats-slug")))
                .andExpect(jsonPath("$.totalClicks", is(3)))
                .andExpect(jsonPath("$.dailyClicks", hasSize(7)))
                .andExpect(jsonPath("$.dailyClicks[6].date", is(today.toLocalDate().toString())))
                .andExpect(jsonPath("$.dailyClicks[6].count", is(2)))
                .andExpect(jsonPath("$.dailyClicks[4].date", is(twoDaysAgo.toLocalDate().toString())))
                .andExpect(jsonPath("$.dailyClicks[4].count", is(1)))
                .andExpect(jsonPath("$.dailyClicks[5].count", is(0)));
    }

    @Test
    void getUrlStats_shouldFailWithForbidden_whenUserDoesNotOwnTheSlug() throws Exception {
        User owner = createTestUser("owner", "owner@test.com", "password123", "Polaris");
        createTestUrl("https://secret.com", "secret-slug", owner);

        String attackerToken = getAccessToken("attacker", "attacker@test.com", "password123", "Polaris");

        mockMvc.perform(get("/api/stats/secret-slug")
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isForbidden());
    }

    private void createClick(Url url, LocalDateTime timestamp) {
        Click click = new Click();
        click.setUrl(url);
        click.setIpAddress("1.1.1.1");
        click.setTimestamp(timestamp);
        clickRepository.save(click);
    }
}