package com.astro.url.creation;

import com.astro.shared.exceptions.RateLimitExceededException;
import com.astro.shared.exceptions.UrlAuthorizationException;
import com.astro.url.dto.UrlResponse;
import com.astro.url.dto.UrlShortenRequest;
import com.astro.url.mapper.UrlMapper;
import com.astro.url.model.Url;
import com.astro.url.repository.UrlRepository;
import com.astro.url.service.RateLimitingService;
import com.astro.url.service.SlugService;
import com.astro.url.validation.ExpirationValidationService;
import com.astro.url.validation.UrlValidationService;
import com.astro.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final ExpirationValidationService expirationValidationService;
    private final RateLimitingService rateLimitingService;
    private final PasswordEncoder passwordEncoder;
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
        LocalDateTime expirationDate = expirationValidationService.determineExpirationDate(request.getExpirationDate(), user);

        Url newUrl = persistUrl(request.getOriginalUrl(), slug, expirationDate, user, request.getPassword());
        return urlMapper.toUrlResponse(newUrl, getBaseUrl(requestUrl));
    }

    private UrlResponse createForAnonymous(UrlShortenRequest request, String ipAddress, String requestUrl) {
        if (request.getExpirationDate() != null) {
            throw new UrlAuthorizationException("Anonymous users cannot set a custom expiration date.");
        }
        if (!rateLimitingService.isAllowedForAnonymous(ipAddress)) {
            throw new RateLimitExceededException("Rate limit of 7 URLs per day exceeded for your IP address.");
        }
        String slug = generateUniqueRandomSlug(6);
        LocalDateTime expirationDate = LocalDateTime.now(clock).plusDays(5);
        Url newUrl = persistUrl(request.getOriginalUrl(), slug, expirationDate, null, null);
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

    private Url persistUrl(String originalUrl, String slug, LocalDateTime expirationDate, User user, String password) {
        Url newUrl = new Url();
        newUrl.setOriginalUrl(originalUrl);
        newUrl.setSlug(slug);
        newUrl.setUser(user);
        newUrl.setExpirationDate(expirationDate);

        if (StringUtils.hasText(password)) {
            if (user == null) { throw new IllegalStateException("Anonymous users cannot set passwords."); }
            urlValidationService.checkCustomSlugPrivilege(user);
            newUrl.setPassword(passwordEncoder.encode(password));
        }

        return urlRepository.save(newUrl);
    }

    private String getBaseUrl(String requestUrl) {
        return requestUrl.substring(0, requestUrl.indexOf("/api/"));
    }
}