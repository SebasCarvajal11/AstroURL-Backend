package com.astro.user.repository;

import com.astro.config.AbstractIntegrationTest;
import com.astro.user.model.User;
import com.astro.user.plan.model.Plan;
import com.astro.user.plan.repository.PlanRepository;
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

    @Test
    void whenSaveAndFindUser_thenItShouldExist() {
        // Given: a plan exists in the database (Liquibase already inserted 'Polaris')
        Optional<Plan> polarisPlanOptional = planRepository.findByName("Polaris");
        assertThat(polarisPlanOptional).isPresent();
        Plan polarisPlan = polarisPlanOptional.get();

        // And: a new user is created
        User newUser = new User();
        newUser.setUsername("testuser");
        newUser.setEmail("testuser@astrourl.com");
        newUser.setPassword("hashedpassword");
        newUser.setPlan(polarisPlan);

        // When: the user is saved
        User savedUser = userRepository.save(newUser);

        // Then: the saved user should have an ID and can be found
        assertThat(savedUser.getId()).isNotNull();
        Optional<User> foundUser = userRepository.findById(savedUser.getId());
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
        assertThat(foundUser.get().getPlan().getName()).isEqualTo("Polaris");
    }
}