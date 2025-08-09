package com.astro.stats.service;

import com.astro.shared.exceptions.UrlNotFoundException;
import com.astro.stats.dto.DailyClickCount;
import com.astro.stats.dto.UrlStatsResponse;
import com.astro.stats.helper.DateAggregationHelper;
import com.astro.stats.mapper.StatsMapper;
import com.astro.stats.model.Click;
import com.astro.stats.repository.ClickRepository;
import com.astro.url.model.Url;
import com.astro.url.repository.UrlRepository;
import com.astro.url.validation.UrlOwnershipValidator;
import com.astro.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final UrlRepository urlRepository;
    private final ClickRepository clickRepository; // Añadir dependencia
    private final DateAggregationHelper dateAggregationHelper; // Añadir dependencia
    private final UrlOwnershipValidator ownershipValidator;
    private final StatsMapper statsMapper;
    private final Clock clock; // Añadir dependencia

    @Transactional(readOnly = true)
    public UrlStatsResponse getStatsForSlug(String slug, User user) {
        Url url = findUrlBySlug(slug);
        ownershipValidator.validate(url, user);

        // CORRECCIÓN: La lógica de negocio ahora vive en el servicio.
        // 1. Obtener la fecha del último clic.
        Optional<Click> lastClick = clickRepository.findTopByUrlOrderByTimestampDesc(url);
        LocalDateTime lastAccessed = lastClick.map(Click::getTimestamp).orElse(null);

        // 2. Obtener y agregar los clics diarios.
        LocalDateTime sevenDaysAgo = LocalDateTime.now(clock).minusDays(6).withHour(0).withMinute(0).withSecond(0);
        List<DailyClickCount> dailyClicks = clickRepository.countClicksByDayForLast7Days(url, sevenDaysAgo);
        List<DailyClickCount> aggregatedClicks = dateAggregationHelper.aggregateClicksOverLast7Days(dailyClicks);

        // 3. Pasar todos los datos al mapper para que solo transforme.
        return statsMapper.toUrlStatsResponse(url, lastAccessed, aggregatedClicks);
    }

    private Url findUrlBySlug(String slug) {
        String lowercaseSlug = slug.toLowerCase();
        return urlRepository.findBySlug(lowercaseSlug)
                .orElseThrow(() -> new UrlNotFoundException(lowercaseSlug));
    }
}