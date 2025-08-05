package com.astro.user.repository;

import com.astro.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // This is the single, authoritative method for fetching a user for authentication purposes.
    // It eagerly fetches the associated Plan to prevent LazyInitializationException.
    @Query("SELECT u FROM User u JOIN FETCH u.plan WHERE u.username = :identifier OR u.email = :identifier")
    Optional<User> findByIdentifierWithPlan(@Param("identifier") String identifier);

    // We keep these for non-authentication scenarios where the plan might not be needed.
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);
}