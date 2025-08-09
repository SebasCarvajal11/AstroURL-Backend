package com.astro.user.repository;

import com.astro.user.model.Email;
import com.astro.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // CORRECCIÓN: El método findByIdentifierWithPlan ahora busca en el campo anidado 'email.value'
    @Query("SELECT u FROM User u JOIN FETCH u.plan WHERE u.username = :identifier OR u.email.value = :identifier")
    Optional<User> findByIdentifierWithPlan(@Param("identifier") String identifier);

    // CORRECCIÓN: El método findByEmail ahora acepta un objeto Email
    Optional<User> findByEmail(Email email);

    // Mantenemos una versión que acepta String para conveniencia y la delegamos
    default Optional<User> findByEmail(String emailString) {
        try {
            return findByEmail(new Email(emailString));
        } catch (IllegalArgumentException e) {
            // Si el formato es inválido, es imposible que exista en la BD.
            return Optional.empty();
        }
    }

    Optional<User> findByUsername(String username);
}