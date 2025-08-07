package com.astro.stats.service;

import com.astro.shared.exceptions.UrlNotFoundException;
import com.astro.stats.dto.UrlStatsResponse;
import com.astro.stats.mapper.StatsMapper;
import com.astro.url.model.Url;
import com.astro.url.repository.UrlRepository;
import com.astro.url.validation.UrlOwnershipValidator;
import com.astro.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final UrlRepository urlRepository;
    private final UrlOwnershipValidator ownershipValidator;
    private final StatsMapper statsMapper;

    @Transactional(readOnly = true)
    public UrlStatsResponse getStatsForSlug(String slug, User user) {
        Url url = findUrlBySlug(slug);
        ownershipValidator.validate(url, user);
        return statsMapper.toUrlStatsResponse(url);
    }

    private Url findUrlBySlug(String slug) {
        String lowercaseSlug = slug.toLowerCase();
        return urlRepository.findBySlug(lowercaseSlug)
                .orElseThrow(() -> new UrlNotFoundException(lowercaseSlug));
    }
}