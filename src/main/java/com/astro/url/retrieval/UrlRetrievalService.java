package com.astro.url.retrieval;

import com.astro.config.RedisKeyManager; // Importar
import com.astro.shared.exceptions.UrlExpiredException;
import com.astro.shared.exceptions.UrlNotFoundException;
import com.astro.url.dto.UrlResponse;
import com.astro.url.event.UrlAccessedEvent;
import com.astro.url.mapper.UrlMapper;
import com.astro.url.model.Url;
import com.astro.url.protection.UrlProtectionService;
import com.astro.url.repository.UrlRepository;
import com.astro.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UrlRetrievalService {

    private final UrlRepository urlRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final UrlProtectionService urlProtectionService;
    private final Clock clock;
    private final UrlMapper urlMapper;
    private final RedisKeyManager redisKeyManager; // Inyectar

    @Transactional(readOnly = true)
    public String getOriginalUrlAndLogClick(String slug, String ipAddress, String userAgent, String password) {
        // CORRECCIÓN: Usar el key manager
        String cacheKey = redisKeyManager.getUrlCacheKey(slug);
        String cachedUrl = redisTemplate.opsForValue().get(cacheKey);

        if (cachedUrl != null) {
            handleCacheHit(slug, ipAddress, userAgent);
            return cachedUrl;
        }

        return handleCacheMiss(slug, ipAddress, userAgent, password);
    }

    @Transactional(readOnly = true)
    public Page<UrlResponse> getUrlsForUser(User user, Pageable pageable, String requestUrl) {
        Page<Url> urlPage = urlRepository.findByUser(user, pageable);
        String baseUrl = requestUrl.substring(0, requestUrl.indexOf("/api/"));
        return urlPage.map(url -> urlMapper.toUrlResponse(url, baseUrl));
    }

    private void handleCacheHit(String slug, String ipAddress, String userAgent) {
        urlRepository.findBySlug(slug.toLowerCase()).ifPresent(url -> {
            if (url.getExpirationDate().isAfter(LocalDateTime.now(clock))) {
                eventPublisher.publishEvent(new UrlAccessedEvent(url, ipAddress, userAgent));
            }
        });
    }

    private String handleCacheMiss(String slug, String ipAddress, String userAgent, String password) {
        Url url = findAndValidateUrl(slug);
        urlProtectionService.checkProtectionStatus(url, password);
        cacheUrl(url);
        eventPublisher.publishEvent(new UrlAccessedEvent(url, ipAddress, userAgent));
        return url.getOriginalUrl();
    }

    private Url findAndValidateUrl(String slug) {
        Url url = urlRepository.findBySlug(slug.toLowerCase())
                .orElseThrow(() -> new UrlNotFoundException(slug));
        if (url.getExpirationDate().isBefore(LocalDateTime.now(clock))) {
            // CORRECCIÓN: Usar el key manager
            String cacheKey = redisKeyManager.getUrlCacheKey(slug);
            redisTemplate.delete(cacheKey);
            throw new UrlExpiredException(slug);
        }
        return url;
    }

    private void cacheUrl(Url url) {
        long ttl = Duration.between(LocalDateTime.now(clock), url.getExpirationDate()).getSeconds();
        if (ttl > 0) {
            // CORRECCIÓN: Usar el key manager
            String cacheKey = redisKeyManager.getUrlCacheKey(url.getSlug());
            redisTemplate.opsForValue().set(cacheKey, url.getOriginalUrl(), ttl + 60, TimeUnit.SECONDS);
        }
    }
}