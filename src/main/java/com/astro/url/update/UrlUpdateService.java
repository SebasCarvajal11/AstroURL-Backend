package com.astro.url.update;

import com.astro.config.RedisKeyManager; // Importar
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
    private final RedisKeyManager redisKeyManager; // Inyectar

    @Transactional
    public UrlResponse updateUrl(Long urlId, UrlUpdateRequest request, User user, String requestUrl) {
        Url url = findUrlById(urlId);
        ownershipValidator.validate(url, user);

        if (request.getPassword() != null || StringUtils.hasText(request.getSlug())) {
            validationService.checkCustomSlugPrivilege(user);
        }

        String oldSlug = url.getSlug();

        url.changeSlug(request.getSlug(), () -> validationService.isSlugAvailable(request.getSlug()));
        url.updateOriginalUrl(request.getOriginalUrl());
        url.updateExpirationDate(request.getExpirationDate());
        url.updatePassword(request.getPassword(), passwordEncoder::encode);

        if (!oldSlug.equalsIgnoreCase(url.getSlug())) {
            invalidateCacheForSlug(oldSlug);
        }

        Url updatedUrl = urlRepository.save(url);
        String baseUrl = requestUrl.substring(0, requestUrl.indexOf("/api/"));
        return urlMapper.toUrlResponse(updatedUrl, baseUrl);
    }

    private Url findUrlById(Long id) {
        return urlRepository.findById(id)
                .orElseThrow(() -> new UrlNotFoundException("URL with ID " + id + " not found."));
    }

    private void invalidateCacheForSlug(String slug) {
        // CORRECCIÓN: Usar el key manager
        String cacheKey = redisKeyManager.getUrlCacheKey(slug);
        redisTemplate.delete(cacheKey);
    }
}