package com.astro.url.scheduler;

import com.astro.config.AbstractIntegrationTest;
import com.astro.url.model.Url;
import com.astro.url.repository.UrlRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
class ExpiredUrlCleanupTaskTest extends AbstractIntegrationTest {

    @Autowired
    private ExpiredUrlCleanupTask cleanupTask;

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired // Necesitamos el PasswordEncoder real del contexto de Spring
    private PasswordEncoder passwordEncoder;

    @MockBean
    private Clock clock;

    @Test
    void cleanupExpiredUrls_shouldDeleteExpiredUrlsAndCache() {
        // Given: The current time is fixed
        Instant now = Instant.parse("2025-01-10T00:00:00Z");
        when(clock.instant()).thenReturn(now);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        // CORRECCIÓN: Usamos el factory method para crear las URLs de prueba
        // And: One expired URL with a cached entry
        Url expiredUrl = Url.createAnonymousUrl(
                "https://expired.com",
                "expired",
                LocalDateTime.now(clock).minusDays(1)
        );
        urlRepository.save(expiredUrl);
        redisTemplate.opsForValue().set("url:slug:expired", "https://expired.com");

        // And: One active URL with a cached entry
        Url activeUrl = Url.createAnonymousUrl(
                "https://active.com",
                "active",
                LocalDateTime.now(clock).plusDays(1)
        );
        urlRepository.save(activeUrl);
        redisTemplate.opsForValue().set("url:slug:active", "https://active.com");

        // When
        cleanupTask.cleanupExpiredUrls();

        // Then
        assertThat(urlRepository.findBySlug("expired")).isEmpty();
        assertThat(redisTemplate.hasKey("url:slug:expired")).isFalse();

        assertThat(urlRepository.findBySlug("active")).isPresent();
        assertThat(redisTemplate.hasKey("url:slug:active")).isTrue();
    }
}