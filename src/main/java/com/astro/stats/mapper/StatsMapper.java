package com.astro.stats.mapper;

import com.astro.stats.dto.DailyClickCount;
import com.astro.stats.dto.UrlStatsResponse;
import com.astro.url.model.Url;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StatsMapper {

    // El mapper ya no necesita dependencias de repositorios o helpers.

    /**
     * CORRECCIÓN: El método ahora es más puro. Recibe todos los datos que necesita
     * como parámetros y su única responsabilidad es transformar esos datos en el DTO de respuesta.
     * @param url La entidad URL.
     * @param lastAccessed La fecha del último acceso (calculada por el servicio).
     * @param aggregatedClicks La lista de clics diarios (calculada y agregada por el servicio).
     * @return El DTO de respuesta completamente mapeado.
     */
    public UrlStatsResponse toUrlStatsResponse(Url url, LocalDateTime lastAccessed, List<DailyClickCount> aggregatedClicks) {
        return UrlStatsResponse.builder()
                .slug(url.getSlug())
                .originalUrl(url.getOriginalUrl())
                .totalClicks(url.getClickCount())
                .lastAccessed(lastAccessed)
                .createdAt(url.getCreatedAt())
                .dailyClicks(aggregatedClicks)
                .build();
    }
}