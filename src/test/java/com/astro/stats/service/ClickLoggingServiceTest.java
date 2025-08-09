package com.astro.stats.service;

import com.astro.stats.model.Click;
import com.astro.stats.repository.ClickRepository;
import com.astro.url.event.UrlAccessedEvent;
import com.astro.url.model.Url;
import com.astro.url.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClickLoggingServiceTest {

    @Mock
    private ClickRepository clickRepository;

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private ClickLoggingService clickLoggingService;

    private static final Instant MOCK_TIME_NOW = Instant.parse("2025-08-10T10:00:00Z");

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(MOCK_TIME_NOW);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    @Test
    void handleUrlAccessed_shouldSaveClickAndUpdateUrlStats() {
        // CORRECCIÓN/REFINAMIENTO: Creamos la Url de una manera que no viole su encapsulación
        // En un test unitario, está bien usar reflexión para setear el ID, ya que no estamos probando
        // la persistencia, sino la lógica de la unidad bajo prueba.
        Url url = Url.createAnonymousUrl("https://test.com", "testslug", LocalDateTime.now().plusDays(1));
        ReflectionTestUtils.setField(url, "id", 1L); // Le asignamos un ID para el mock de findById
        url.setClickCount(5); // Seteamos el estado inicial que necesitamos probar

        String ipAddress = "192.168.1.1";
        String userAgent = "Test Agent";
        UrlAccessedEvent event = new UrlAccessedEvent(url, ipAddress, userAgent);

        when(urlRepository.findById(1L)).thenReturn(Optional.of(url));

        clickLoggingService.handleUrlAccessed(event);

        ArgumentCaptor<Click> clickCaptor = ArgumentCaptor.forClass(Click.class);
        verify(clickRepository).save(clickCaptor.capture());

        Click savedClick = clickCaptor.getValue();
        assertThat(savedClick.getUrl()).isEqualTo(url);
        assertThat(savedClick.getIpAddress()).isEqualTo(ipAddress);
        assertThat(savedClick.getUserAgent()).isEqualTo(userAgent);

        ArgumentCaptor<Url> urlCaptor = ArgumentCaptor.forClass(Url.class);
        verify(urlRepository).save(urlCaptor.capture());

        Url updatedUrl = urlCaptor.getValue();
        assertThat(updatedUrl.getClickCount()).isEqualTo(6);
        assertThat(updatedUrl.getLastAccessedAt()).isEqualTo(LocalDateTime.now(clock));
    }
}