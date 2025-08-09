package com.astro.url.controller;

import com.astro.config.BaseControllerTest;
import com.astro.url.model.Url;
import com.astro.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;


@SpringBootTest
@AutoConfigureMockMvc
class RedirectControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void redirect_shouldSucceed_forValidSlug() throws Exception {
        // CORRECCIÓN: Usamos el factory method para URLs anónimas
        Url url = Url.createAnonymousUrl("https://example.com/valid", "validslug", LocalDateTime.now(clock).plusDays(1));
        urlRepository.save(url);

        mockMvc.perform(get("/r/validslug"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://example.com/valid"));
    }

    @Test
    void redirect_shouldFailWith404_forNonExistentSlug() throws Exception {
        mockMvc.perform(get("/r/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void redirect_shouldFailWith410_forExpiredSlug() throws Exception {
        // CORRECCIÓN: Usamos el factory method para URLs anónimas
        Url url = Url.createAnonymousUrl("https://example.com/expired", "expired", LocalDateTime.now(clock).minusDays(1));
        urlRepository.save(url);

        mockMvc.perform(get("/r/expired"))
                .andExpect(status().isGone());
    }

    @Test
    void redirect_shouldFailWithUnauthorized_whenPasswordIsRequiredButNotProvided() throws Exception {
        createTestUrlWithPassword("https://secret.com", "secret", "password123");

        mockMvc.perform(get("/r/secret"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.passwordRequired", is(true)));
    }

    @Test
    void redirect_shouldSucceed_whenPasswordIsCorrect() throws Exception {
        createTestUrlWithPassword("https://secret.com", "secret-2", "password123");

        mockMvc.perform(get("/r/secret-2")
                        .header("X-Password", "password123"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://secret.com"));
    }

    /**
     * CORRECCIÓN: Este método ahora debe crear un usuario, porque nuestra lógica de dominio
     * enriquecida solo permite contraseñas para usuarios autenticados (plan Sirio).
     * Usamos el factory method para URLs autenticadas.
     */
    private Url createTestUrlWithPassword(String originalUrl, String slug, String rawPassword) {
        // La protección con contraseña requiere un usuario con un plan que lo permita (ej. Sirio)
        User user = createTestUser("pwd-user-" + slug, "pwd-user-" + slug + "@test.com", "defaultPass", "Sirio");

        Url url = Url.createAuthenticatedUrl(
                originalUrl,
                slug,
                user,
                LocalDateTime.now(clock).plusDays(1),
                rawPassword,
                passwordEncoder::encode
        );
        return urlRepository.save(url);
    }
}