package com.astro.url.service;

import com.astro.shared.exceptions.RateLimitExceededException;
import com.astro.stats.service.ClickLoggingService;
import com.astro.url.dto.UrlResponse;
import com.astro.url.dto.UrlShortenRequest;
import com.astro.url.mapper.UrlMapper;
import com.astro.url.model.Url;
import com.astro.url.repository.UrlRepository;
import com.astro.user.model.User;
import com.astro.shared.exceptions.RateLimitExceededException;
import com.astro.shared.exceptions.UrlExpiredException;
import com.astro.shared.exceptions.UrlNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private final SlugService slugService;
    private final RateLimitingService rateLimitingService;
    private final ClickLoggingService clickLoggingService;
    private final Clock clock;
    private final UrlMapper urlMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public UrlResponse shortenUrlForAnonymous(UrlShortenRequest request, String ipAddress, String requestUrl) {
        if (!rateLimitingService.isAllowedForAnonymous(ipAddress)) {
            throw new RateLimitExceededException("Rate limit of 7 URLs per day exceeded for your IP address.");
        }

        Url newUrl = createUrl(request.getOriginalUrl(), 6, 5, null);
        String baseUrl = getBaseUrl(requestUrl);
        return urlMapper.toUrlResponse(newUrl, baseUrl);
    }

    @Transactional
    public UrlResponse shortenUrlForAuthenticatedUser(UrlShortenRequest request, User user, String requestUrl) {
        if (!rateLimitingService.isAllowedForUser(user)) {
            throw new RateLimitExceededException("Your daily URL creation limit has been reached.");
        }

        // For Polaris: slug length = 5, expiration = 14 days
        // This will be dynamic when more plans are added.
        int slugLength = 5; // Could be from user.getPlan().getSlugLength()
        int expirationDays = 14; // Could be from user.getPlan().getUrlExpirationDays()

        Url newUrl = createUrl(request.getOriginalUrl(), slugLength, expirationDays, user);
        String baseUrl = getBaseUrl(requestUrl);
        return urlMapper.toUrlResponse(newUrl, baseUrl);
    }

    private Url createUrl(String originalUrl, int slugLength, int expirationDays, User user) {
        String slug;
        do {
            slug = slugService.generateRandomSlug(slugLength);
        } while (urlRepository.findBySlug(slug).isPresent());

        Url newUrl = new Url();
        newUrl.setOriginalUrl(originalUrl);
        newUrl.setSlug(slug);
        newUrl.setUser(user);
        newUrl.setExpirationDate(LocalDateTime.now(clock).plusDays(expirationDays));

        return urlRepository.save(newUrl);
    }

    private String getBaseUrl(String requestUrl) {
        // Extracts "http://localhost:8080" from "http://localhost:8080/api/url"
        return requestUrl.substring(0, requestUrl.indexOf("/api/"));
    }

    @Transactional(readOnly = true)
    public String getOriginalUrlAndLogClick(String slug, String ipAddress, String userAgent) {
        String lowercaseSlug = slug.toLowerCase();
        String cacheKey = "url:slug:" + lowercaseSlug;

        // 1. Check cache first
        String originalUrl = redisTemplate.opsForValue().get(cacheKey);
        if (originalUrl != null) {
            // Asynchronously log the click for cached URL
            urlRepository.findBySlug(lowercaseSlug).ifPresent(url ->
                    clickLoggingService.logClick(url, ipAddress, userAgent)
            );
            return originalUrl;
        }

        // 2. If not in cache, fetch from DB
        Url url = urlRepository.findBySlug(lowercaseSlug)
                .orElseThrow(() -> new UrlNotFoundException(lowercaseSlug));

        // 3. Validate expiration
        if (url.getExpirationDate().isBefore(LocalDateTime.now(clock))) {
            throw new UrlExpiredException(lowercaseSlug);
        }

        // 4. Cache the result
        long ttlInSeconds = Duration.between(LocalDateTime.now(clock), url.getExpirationDate()).getSeconds();
        if (ttlInSeconds > 0) {
            redisTemplate.opsForValue().set(cacheKey, url.getOriginalUrl(), ttlInSeconds + 60, TimeUnit.SECONDS); // Add a small buffer
        }

        // 5. Log the click asynchronously
        clickLoggingService.logClick(url, ipAddress, userAgent);

        return url.getOriginalUrl();
    }
}