package com.astro.url.controller;

import com.astro.config.AbstractIntegrationTest;
import com.astro.stats.service.ClickLoggingService;
import com.astro.url.model.Url;
import com.astro.url.repository.UrlRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
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

    @SpyBean
    private ClickLoggingService clickLoggingService;

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

    @Test
    void redirect_shouldUseXForwardedForHeader_whenPresent() throws Exception {
        // Given
        Url url = new Url();
        url.setSlug("proxytest");
        url.setOriginalUrl("https://example.com/proxy");
        url.setExpirationDate(LocalDateTime.now().plusDays(1));
        urlRepository.save(url);

        String proxyIp = "10.0.0.1";

        // When
        mockMvc.perform(get("/r/proxytest")
                        .header("X-Forwarded-For", proxyIp))
                .andExpect(status().isMovedPermanently());

        // Then: Verify that the async service was called with the correct IP
        ArgumentCaptor<String> ipCaptor = ArgumentCaptor.forClass(String.class);
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                verify(clickLoggingService, atLeastOnce()).logClick(
                        org.mockito.ArgumentMatchers.any(Url.class),
                        ipCaptor.capture(),
                        org.mockito.ArgumentMatchers.any()
                )
        );

        assertThat(ipCaptor.getValue()).isEqualTo(proxyIp);
    }
}