package com.astro.stats.service;

import com.astro.config.RedisKeyManager; // Importar
import com.astro.stats.dto.UrlClickCount;
import com.astro.stats.repository.ClickRepository;
import com.astro.url.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UrlStatsBatchUpdater {

    private static final Logger logger = LoggerFactory.getLogger(UrlStatsBatchUpdater.class);
    // ELIMINAR: private static final String LAST_PROCESSED_TIMESTAMP_KEY = "stats:lastProcessedClickTimestamp";

    private final ClickRepository clickRepository;
    private final UrlRepository urlRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final Clock clock;
    private final RedisKeyManager redisKeyManager; // Inyectar

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void updateUrlStats() {
        logger.info("Starting URL stats update batch job.");

        // CORRECCIÓN: Usar el key manager
        String lastProcessedKey = redisKeyManager.getLastProcessedTimestampKey();
        String lastProcessedValue = redisTemplate.opsForValue().get(lastProcessedKey);
        LocalDateTime lastProcessedTimestamp = lastProcessedValue != null ?
                LocalDateTime.parse(lastProcessedValue) :
                LocalDateTime.of(2000, 1, 1, 0, 0);

        List<UrlClickCount> newClickCounts = clickRepository.getAggregatedClickCountsSince(lastProcessedTimestamp);

        if (newClickCounts.isEmpty()) {
            logger.info("No new clicks to process. Batch job finished.");
            return;
        }

        logger.info("Found {} URLs with new clicks to update.", newClickCounts.size());
        LocalDateTime latestTimestampInBatch = lastProcessedTimestamp;

        for (UrlClickCount count : newClickCounts) {
            urlRepository.incrementClickCountAndSetLastAccessed(
                    count.getUrlId(),
                    count.getClickCount(),
                    count.getMaxTimestamp()
            );

            if (count.getMaxTimestamp().isAfter(latestTimestampInBatch)) {
                latestTimestampInBatch = count.getMaxTimestamp();
            }
        }

        // CORRECCIÓN: Usar el key manager
        redisTemplate.opsForValue().set(lastProcessedKey, latestTimestampInBatch.toString());

        logger.info("Successfully updated stats for {} URLs. Batch job finished.", newClickCounts.size());
    }
}