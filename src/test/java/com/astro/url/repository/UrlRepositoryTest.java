package com.astro.url.repository;

import com.astro.config.AbstractIntegrationTest;
import com.astro.url.model.Url;
import com.astro.user.model.User;
import com.astro.user.plan.model.Plan;
import com.astro.user.repository.UserRepository;
import com.astro.user.plan.repository.PlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UrlRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UrlRepository urlRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlanRepository planRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        Plan plan = planRepository.findByName("Polaris").orElseThrow();
        User user = new User();
        user.setUsername("url-user");
        user.setEmail("url-user@test.com");
        user.setPassword("password");
        user.setPlan(plan);
        testUser = userRepository.save(user);
    }

    @Test
    void whenSaveAndFindBySlug_thenUrlIsFound() {
        // Given
        Url url = new Url();
        url.setSlug("testslug");
        url.setOriginalUrl("https://google.com");
        url.setExpirationDate(LocalDateTime.now().plusDays(1));
        url.setUser(testUser);

        // When
        urlRepository.save(url);
        Optional<Url> foundUrlOpt = urlRepository.findBySlug("testslug");

        // Then
        assertThat(foundUrlOpt).isPresent();
        Url foundUrl = foundUrlOpt.get();
        assertThat(foundUrl.getOriginalUrl()).isEqualTo("https://google.com");
        assertThat(foundUrl.getUser().getUsername()).isEqualTo("url-user");
    }
}