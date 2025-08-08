package com.astro.url.creation;

import com.astro.shared.exceptions.RateLimitExceededException;
import com.astro.url.dto.UrlResponse;
import com.astro.url.dto.UrlShortenRequest;
import com.astro.url.mapper.UrlMapper;
import com.astro.url.model.Url;
import com.astro.url.repository.UrlRepository;
import com.astro.url.service.RateLimitingService;
import com.astro.url.service.SlugService;
import com.astro.url.validation.UrlValidationService;
import com.astro.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UrlCreationService {

    private final UrlRepository urlRepository;
    private final SlugService slugService;
    private final UrlMapper urlMapper;
    private final UrlValidationService urlValidationService;
    private final RateLimitingService rateLimitingService;
    private final Clock clock;

    @Transactional
    public UrlResponse createShortUrl(UrlShortenRequest request, User user, String ipAddress, String requestUrl) {
        if (user != null) {
            return createForAuthenticated(request, user, requestUrl);
        } else {
            return createForAnonymous(request, ipAddress, requestUrl);
        }
    }

    private UrlResponse createForAuthenticated(UrlShortenRequest request, User user, String requestUrl) {
        if (!rateLimitingService.isAllowedForUser(user)) {
            throw new RateLimitExceededException("Your daily URL creation limit has been reached.");
        }
        String slug = determineSlug(request.getSlug(), user);
        int expirationDays = 14;
        Url newUrl = persistUrl(request.getOriginalUrl(), slug, expirationDays, user);
        return urlMapper.toUrlResponse(newUrl, getBaseUrl(requestUrl));
    }

    private UrlResponse createForAnonymous(UrlShortenRequest request, String ipAddress, String requestUrl) {
        if (!rateLimitingService.isAllowedForAnonymous(ipAddress)) {
            throw new RateLimitExceededException("Rate limit of 7 URLs per day exceeded for your IP address.");
        }
        String slug = generateUniqueRandomSlug(6);
        Url newUrl = persistUrl(request.getOriginalUrl(), slug, 5, null);
        return urlMapper.toUrlResponse(newUrl, getBaseUrl(requestUrl));
    }

    private String determineSlug(String customSlug, User user) {
        if (StringUtils.hasText(customSlug)) {
            urlValidationService.checkCustomSlugPrivilege(user);
            urlValidationService.checkSlugAvailability(customSlug);
            return customSlug;
        }
        return generateUniqueRandomSlug(5);
    }

    private String generateUniqueRandomSlug(int length) {
        String slug;
        do {
            slug = slugService.generateRandomSlug(length);
        } while (!urlValidationService.isSlugAvailable(slug));
        return slug;
    }

    private Url persistUrl(String originalUrl, String slug, int expirationDays, User user) {
        Url newUrl = new Url();
        newUrl.setOriginalUrl(originalUrl);
        newUrl.setSlug(slug);
        newUrl.setUser(user);
        newUrl.setExpirationDate(LocalDateTime.now(clock).plusDays(expirationDays));
        return urlRepository.save(newUrl);
    }

    private String getBaseUrl(String requestUrl) {
        return requestUrl.substring(0, requestUrl.indexOf("/api/"));
    }
}