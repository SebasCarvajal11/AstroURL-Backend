package com.astro.url.repository;

import com.astro.url.model.Url;
import com.astro.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {
    Optional<Url> findBySlug(String slug);
    Page<Url> findByExpirationDateBefore(LocalDateTime dateTime, Pageable pageable);
    Page<Url> findByUser(User user, Pageable pageable);

    @Modifying
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + 1 WHERE u.id = :urlId")
    void incrementClickCount(@Param("urlId") Long urlId);
}