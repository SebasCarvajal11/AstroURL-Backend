package com.astro.url.update;

import com.astro.shared.exceptions.UrlNotFoundException;
import com.astro.url.dto.UrlResponse;
import com.astro.url.dto.UrlUpdateRequest;
import com.astro.url.mapper.UrlMapper;
import com.astro.url.model.Url;
import com.astro.url.repository.UrlRepository;
import com.astro.url.validation.UrlOwnershipValidator;
import com.astro.url.validation.UrlValidationService;
import com.astro.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UrlUpdateService {

    private final UrlRepository urlRepository;
    private final UrlOwnershipValidator ownershipValidator;
    private final UrlValidationService validationService;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final UrlMapper urlMapper;

    @Transactional
    public UrlResponse updateUrl(Long urlId, UrlUpdateRequest request, User user, String requestUrl) {
        Url url = findUrlById(urlId);
        ownershipValidator.validate(url, user);

        applySlugUpdate(url, request.getSlug());
        applyOriginalUrlUpdate(url, request.getOriginalUrl());
        applyExpirationUpdate(url, request.getExpirationDate());
        applyPasswordUpdate(url, request.getPassword());

        Url updatedUrl = urlRepository.save(url);
        String baseUrl = requestUrl.substring(0, requestUrl.indexOf("/api/"));
        return urlMapper.toUrlResponse(updatedUrl, baseUrl);
    }

    private Url findUrlById(Long id) {
        return urlRepository.findById(id)
                .orElseThrow(() -> new UrlNotFoundException("URL with ID " + id + " not found."));
    }

    private void applySlugUpdate(Url url, String newSlug) {
        if (StringUtils.hasText(newSlug) && !newSlug.equalsIgnoreCase(url.getSlug())) {
            validationService.checkSlugAvailability(newSlug);
            invalidateCacheForSlug(url.getSlug());
            url.setSlug(newSlug.toLowerCase());
        }
    }

    private void applyOriginalUrlUpdate(Url url, String newOriginalUrl) {
        if (StringUtils.hasText(newOriginalUrl)) {
            url.setOriginalUrl(newOriginalUrl);
        }
    }

    private void applyExpirationUpdate(Url url, java.time.LocalDateTime newExpirationDate) {
        if (newExpirationDate != null) {
            url.setExpirationDate(newExpirationDate);
        }
    }

    private void applyPasswordUpdate(Url url, String newPassword) {
        if (StringUtils.hasText(newPassword)) {
            url.setPassword(passwordEncoder.encode(newPassword));
        } else if (newPassword != null) { // Handle empty string to remove password
            url.setPassword(null);
        }
    }

    private void invalidateCacheForSlug(String slug) {
        String cacheKey = "url:slug:" + slug.toLowerCase();
        redisTemplate.delete(cacheKey);
    }
}