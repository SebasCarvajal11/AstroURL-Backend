package com.astro.stats.repository;

import com.astro.stats.model.Click;
import com.astro.url.model.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClickRepository extends JpaRepository<Click, Long> {
    Optional<Click> findTopByUrlOrderByTimestampDesc(Url url);
}