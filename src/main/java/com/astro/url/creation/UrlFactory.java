package com.astro.url.creation;

import com.astro.shared.exceptions.RateLimitExceededException;
import com.astro.shared.exceptions.UrlAuthorizationException;
import com.astro.url.dto.UrlShortenRequest;
import com.astro.url.model.Url;
import com.astro.url.service.RateLimitingService;
import com.astro.url.service.SlugService;
import com.astro.url.validation.ExpirationValidationService;
import com.astro.url.validation.UrlValidationService;
import com.astro.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class UrlFactory {

    private final SlugService slugService;
    private final UrlValidationService urlValidationService;
    private final ExpirationValidationService expirationValidationService;
    private final RateLimitingService rateLimitingService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public Url createForAuthenticated(UrlShortenRequest request, User user) {
        if (!rateLimitingService.isAllowedForUser(user)) {
            throw new RateLimitExceededException("Your daily URL creation limit has been reached.");
        }
        if (StringUtils.hasText(request.getPassword())) {
            urlValidationService.checkPasswordPrivilege(user);
        }

        String slug = determineSlug(request.getSlug(), user);
        LocalDateTime expirationDate = expirationValidationService.determineExpirationDate(request.getExpirationDate(), user);
        Function<String, String> encoder = passwordEncoder::encode;

        return Url.createAuthenticatedUrl(request.getOriginalUrl(), slug, user, expirationDate, request.getPassword(), encoder);
    }

    public Url createForAnonymous(UrlShortenRequest request, String ipAddress) {
        if (request.getExpirationDate() != null) {
            throw new UrlAuthorizationException("Anonymous users cannot set a custom expiration date.");
        }
        if (StringUtils.hasText(request.getSlug()) || StringUtils.hasText(request.getPassword())) {
            throw new UrlAuthorizationException("Anonymous users cannot set custom slugs or passwords.");
        }
        if (!rateLimitingService.isAllowedForAnonymous(ipAddress)) {
            throw new RateLimitExceededException("Rate limit of 7 URLs per day exceeded for your IP address.");
        }

        String slug = generateUniqueRandomSlug(6);
        LocalDateTime expirationDate = LocalDateTime.now(clock).plusDays(5);
        return Url.createAnonymousUrl(request.getOriginalUrl(), slug, expirationDate);
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
}