package com.astro.stats.mapper;

import com.astro.stats.dto.DailyClickCount;
import com.astro.stats.dto.UrlStatsResponse;
import com.astro.stats.helper.DateAggregationHelper;
import com.astro.stats.model.Click;
import com.astro.stats.repository.ClickRepository;
import com.astro.url.model.Url;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StatsMapper {

    private final ClickRepository clickRepository;
    private final DateAggregationHelper dateAggregationHelper;
    private final Clock clock;

    public UrlStatsResponse toUrlStatsResponse(Url url) {
        Optional<Click> lastClick = clickRepository.findTopByUrlOrderByTimestampDesc(url);
        LocalDateTime lastAccessed = lastClick.map(Click::getTimestamp).orElse(null);

        List<DailyClickCount> dailyClicks = getDailyClicks(url);
        List<DailyClickCount> aggregatedClicks = dateAggregationHelper.aggregateClicksOverLast7Days(dailyClicks);

        return UrlStatsResponse.builder()
                .slug(url.getSlug())
                .originalUrl(url.getOriginalUrl())
                .totalClicks(url.getClickCount())
                .lastAccessed(lastAccessed)
                .createdAt(url.getCreatedAt())
                .dailyClicks(aggregatedClicks)
                .build();
    }

    private List<DailyClickCount> getDailyClicks(Url url) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now(clock).minusDays(6).withHour(0).withMinute(0).withSecond(0);
        return clickRepository.countClicksByDayForLast7Days(url, sevenDaysAgo);
    }
}