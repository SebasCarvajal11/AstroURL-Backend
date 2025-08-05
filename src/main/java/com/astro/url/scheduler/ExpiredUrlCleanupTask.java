package com.astro.url.scheduler;

import com.astro.url.model.Url;
import com.astro.url.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ExpiredUrlCleanupTask {

    private static final Logger logger = LoggerFactory.getLogger(ExpiredUrlCleanupTask.class);
    private static final int BATCH_SIZE = 100;

    private final UrlRepository urlRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final Clock clock;

    @Scheduled(cron = "0 0 0 * * ?") // Runs daily at midnight UTC
    @Transactional
    public void cleanupExpiredUrls() {
        logger.info("Starting expired URL cleanup task.");
        int totalDeleted = 0;
        Page<Url> expiredUrlsPage;

        do {
            PageRequest pageRequest = PageRequest.of(0, BATCH_SIZE);
            expiredUrlsPage = urlRepository.findByExpirationDateBefore(LocalDateTime.now(clock), pageRequest);

            List<Url> urlsToDelete = expiredUrlsPage.getContent();
            if (!urlsToDelete.isEmpty()) {
                // Invalidate cache entries
                List<String> cacheKeys = urlsToDelete.stream()
                        .map(url -> "url:slug:" + url.getSlug().toLowerCase())
                        .toList();
                redisTemplate.delete(cacheKeys);

                // Delete from database
                urlRepository.deleteAllInBatch(urlsToDelete);

                totalDeleted += urlsToDelete.size();
                logger.info("Deleted a batch of {} expired URLs.", urlsToDelete.size());
            }

        } while (expiredUrlsPage.hasNext());

        if (totalDeleted > 0) {
            logger.info("Expired URL cleanup task finished. Total URLs deleted: {}", totalDeleted);
        } else {
            logger.info("Expired URL cleanup task finished. No expired URLs found.");
        }
    }
}