package com.astro.stats.service;

import com.astro.stats.model.Click;
import com.astro.stats.repository.ClickRepository;
import com.astro.url.event.UrlAccessedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClickLoggingService {

    private final ClickRepository clickRepository;
    private static final Logger logger = LoggerFactory.getLogger(ClickLoggingService.class);

    /**
     * Este método escucha el evento de acceso a una URL.
     * Su única responsabilidad ahora es registrar el clic en la tabla 'clicks'.
     * La actualización del contador en la tabla 'urls' ha sido desacoplada
     * a un proceso por lotes para mejorar el rendimiento.
     *
     * @param event El evento que contiene la información del acceso a la URL.
     */
    @Async("taskExecutor")
    @EventListener
    @Transactional
    public void handleUrlAccessed(UrlAccessedEvent event) {
        try {
            Click click = new Click();
            click.setUrl(event.url());
            click.setIpAddress(event.ipAddress());
            click.setUserAgent(event.userAgent());
            clickRepository.save(click);

        } catch (Exception e) {
            logger.error("Failed to log click asynchronously for slug: {}", event.url().getSlug(), e);
        }
    }
}