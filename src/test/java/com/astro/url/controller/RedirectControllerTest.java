package com.astro.url.controller;

import com.astro.config.AbstractIntegrationTest;
import com.astro.url.model.Url;
import com.astro.url.repository.UrlRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RedirectControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UrlRepository urlRepository;

    @Test
    void redirect_shouldSucceed_forValidSlug() throws Exception {
        Url url = new Url();
        url.setSlug("validslug");
        url.setOriginalUrl("https://example.com/valid");
        url.setExpirationDate(LocalDateTime.now().plusDays(1));
        urlRepository.save(url);

        mockMvc.perform(get("/r/validslug"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://example.com/valid"));
    }

    @Test
    void redirect_shouldFailWith404_forNonExistentSlug() throws Exception {
        mockMvc.perform(get("/r/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void redirect_shouldFailWith410_forExpiredSlug() throws Exception {
        Url url = new Url();
        url.setSlug("expired");
        url.setOriginalUrl("https://example.com/expired");
        url.setExpirationDate(LocalDateTime.now().minusDays(1));
        urlRepository.save(url);

        mockMvc.perform(get("/r/expired"))
                .andExpect(status().isGone());
    }
}