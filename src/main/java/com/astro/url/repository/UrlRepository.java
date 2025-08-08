package com.astro.url.repository;

import com.astro.url.model.Url;
import com.astro.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {
    Optional<Url> findBySlug(String slug);
    Page<Url> findByExpirationDateBefore(LocalDateTime dateTime, Pageable pageable);
    Page<Url> findByUser(User user, Pageable pageable);
    List<Url> findByUserAndExpirationDateAfter(User user, LocalDateTime dateTime);
}