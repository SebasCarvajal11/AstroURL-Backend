package com.astro.stats.service;

import com.astro.stats.model.Click;
import com.astro.stats.repository.ClickRepository;
import com.astro.url.model.Url;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClickLoggingService {

    private final ClickRepository clickRepository;

    @Async
    public void logClick(Url url, String ipAddress, String userAgent) {
        Click click = new Click();
        click.setUrl(url);
        click.setIpAddress(ipAddress);
        click.setUserAgent(userAgent);
        clickRepository.save(click);
    }
}