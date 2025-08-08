package com.astro.stats.service;

import com.astro.stats.model.Click;
import com.astro.stats.repository.ClickRepository;
import com.astro.url.model.Url;
import com.astro.url.repository.UrlRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ClickLoggingService {

    private final ClickRepository clickRepository;
    private final UrlRepository urlRepository;
    private final Clock clock;
    private static final Logger logger = LoggerFactory.getLogger(ClickLoggingService.class);

    @Async("taskExecutor")
    @Transactional
    public void logClick(Url url, String ipAddress, String userAgent) {
        try {
            // Guardar el evento de clic
            Click click = new Click();
            click.setUrl(url);
            click.setIpAddress(ipAddress);
            click.setUserAgent(userAgent);
            clickRepository.save(click);

            // Actualizar la entidad Url de forma segura
            urlRepository.findById(url.getId()).ifPresent(urlToUpdate -> {
                urlToUpdate.setClickCount(urlToUpdate.getClickCount() + 1);
                urlToUpdate.setLastAccessedAt(LocalDateTime.now(clock));
                urlRepository.save(urlToUpdate);
            });
        } catch (Exception e) {
            logger.error("Failed to log click asynchronously for slug: {}", url.getSlug(), e);
        }
    }
}