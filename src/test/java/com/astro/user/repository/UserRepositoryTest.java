package com.astro.user.repository;

import com.astro.config.AbstractIntegrationTest;
import com.astro.user.model.User;
import com.astro.user.plan.model.Plan;
import com.astro.user.plan.repository.PlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    private Plan polarisPlan;

    @BeforeEach
    void setUp() {
        // Liquibase inserts this plan, we just fetch it for our tests.
        polarisPlan = planRepository.findByName("Polaris")
                .orElseThrow(() -> new IllegalStateException("Default plan 'Polaris' not found."));
    }

    @Test
    void whenSaveUser_thenTimestampsAreSetAndFindersWork() {
        // Given
        User newUser = new User();
        newUser.setUsername("test-user-01");
        newUser.setEmail("testuser01@astrourl.com");
        newUser.setPassword("hashedpassword");
        newUser.setPlan(polarisPlan);

        // When
        User savedUser = userRepository.save(newUser);

        // Then: Basic properties are saved correctly
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isEqualTo(savedUser.getUpdatedAt());

        // And: It can be found by username
        Optional<User> foundByUsername = userRepository.findByUsername("test-user-01");
        assertThat(foundByUsername).isPresent();
        assertThat(foundByUsername.get().getEmail()).isEqualTo("testuser01@astrourl.com");

        // And: It can be found by email
        Optional<User> foundByEmail = userRepository.findByEmail("testuser01@astrourl.com");
        assertThat(foundByEmail).isPresent();
        assertThat(foundByEmail.get().getUsername()).isEqualTo("test-user-01");
    }

    @Test
    void findByUsername_shouldReturnEmpty_whenUserDoesNotExist() {
        // When
        Optional<User> foundUser = userRepository.findByUsername("non-existent-user");

        // Then
        assertThat(foundUser).isNotPresent();
    }

    @Test
    void findByEmail_shouldReturnEmpty_whenEmailDoesNotExist() {
        // When
        Optional<User> foundUser = userRepository.findByEmail("non-existent@email.com");

        // Then
        assertThat(foundUser).isNotPresent();
    }
}