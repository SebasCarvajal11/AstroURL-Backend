package com.astro.url.repository;

import com.astro.url.model.Url;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {
    Optional<Url> findBySlug(String slug);
    Page<Url> findByExpirationDateBefore(LocalDateTime dateTime, Pageable pageable);
}