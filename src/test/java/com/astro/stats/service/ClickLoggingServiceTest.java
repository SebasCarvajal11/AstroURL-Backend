package com.astro.stats.service;

import com.astro.stats.model.Click;
import com.astro.stats.repository.ClickRepository;
import com.astro.url.model.Url;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClickLoggingServiceTest {

    @Mock
    private ClickRepository clickRepository;

    @InjectMocks
    private ClickLoggingService clickLoggingService;

    @Test
    void logClick_shouldSaveClickWithCorrectData() {
        // Given
        Url url = new Url();
        url.setSlug("testslug");
        String ipAddress = "192.168.1.1";
        String userAgent = "Test Agent";

        // When
        clickLoggingService.logClick(url, ipAddress, userAgent);

        // Then
        ArgumentCaptor<Click> clickCaptor = ArgumentCaptor.forClass(Click.class);
        verify(clickRepository).save(clickCaptor.capture());

        Click savedClick = clickCaptor.getValue();
        assertThat(savedClick.getUrl()).isEqualTo(url);
        assertThat(savedClick.getIpAddress()).isEqualTo(ipAddress);
        assertThat(savedClick.getUserAgent()).isEqualTo(userAgent);
    }
}