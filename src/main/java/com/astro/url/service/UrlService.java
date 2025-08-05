package com.astro.url.service;

import com.astro.shared.exceptions.RateLimitExceededException;
import com.astro.url.dto.UrlShortenRequest;
import com.astro.url.dto.UrlShortenResponse;
import com.astro.url.model.Url;
import com.astro.url.repository.UrlRepository;
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

    @Transactional
    public UrlShortenResponse shortenUrlForAnonymous(UrlShortenRequest request, String ipAddress, String requestUrl) {
        if (!rateLimitingService.isAllowed(ipAddress)) {
            throw new RateLimitExceededException("Rate limit of 7 URLs per day exceeded for your IP address.");
        }

        String slug;
        do {
            slug = slugService.generateRandomSlug(6);
        } while (urlRepository.findBySlug(slug).isPresent());

        Url newUrl = new Url();
        newUrl.setOriginalUrl(request.getOriginalUrl());
        newUrl.setSlug(slug);
        newUrl.setUser(null); // Anonymous user
        newUrl.setExpirationDate(LocalDateTime.now(clock).plusDays(5));

        urlRepository.save(newUrl);

        // Construct the full short URL
        String domain = requestUrl.substring(0, requestUrl.indexOf("/api/"));
        String shortUrl = domain + "/r/" + slug;

        return new UrlShortenResponse(slug, shortUrl);
    }
}
