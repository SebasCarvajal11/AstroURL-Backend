package com.astro.url.service;

import com.astro.shared.exceptions.RateLimitExceededException;
import com.astro.url.dto.UrlResponse;
import com.astro.url.dto.UrlShortenRequest;
import com.astro.url.mapper.UrlMapper;
import com.astro.url.model.Url;
import com.astro.url.repository.UrlRepository;
import com.astro.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private final SlugService slugService;
    private final RateLimitingService rateLimitingService;
    private final Clock clock;
    private final UrlMapper urlMapper;

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
}