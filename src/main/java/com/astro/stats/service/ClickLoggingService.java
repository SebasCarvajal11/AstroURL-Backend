package com.astro.stats.service;

import com.astro.stats.model.Click;
import com.astro.stats.repository.ClickRepository;
import com.astro.url.model.Url;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClickLoggingService {

    private final ClickRepository clickRepository;
    private static final Logger logger = LoggerFactory.getLogger(ClickLoggingService.class);

    @Async("taskExecutor")
    public void logClick(Url url, String ipAddress, String userAgent) {
        try {
            Click click = new Click();
            click.setUrl(url);
            click.setIpAddress(ipAddress);
            click.setUserAgent(userAgent);
            clickRepository.save(click);
        } catch (Exception e) {
            logger.error("Failed to log click asynchronously for slug: {}", url.getSlug(), e);
        }
    }
}