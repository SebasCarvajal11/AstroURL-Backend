package com.astro.stats.controller;

import com.astro.stats.dto.UrlStatsResponse;
import com.astro.stats.service.StatsService;
import com.astro.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/{slug}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UrlStatsResponse> getUrlStats(
            @PathVariable String slug,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        UrlStatsResponse stats = statsService.getStatsForSlug(slug, user);
        return ResponseEntity.ok(stats);
    }
}