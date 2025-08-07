package com.astro.stats.mapper;

import com.astro.stats.dto.UrlStatsResponse;
import com.astro.stats.model.Click;
import com.astro.stats.repository.ClickRepository;
import com.astro.url.model.Url;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StatsMapper {

    private final ClickRepository clickRepository;

    public UrlStatsResponse toUrlStatsResponse(Url url) {
        Optional<Click> lastClick = clickRepository.findTopByUrlOrderByTimestampDesc(url);
        LocalDateTime lastAccessed = lastClick.map(Click::getTimestamp).orElse(null);

        return UrlStatsResponse.builder()
                .slug(url.getSlug())
                .originalUrl(url.getOriginalUrl())
                .totalClicks(url.getClickCount())
                .lastAccessed(lastAccessed)
                .createdAt(url.getCreatedAt())
                .build();
    }
}