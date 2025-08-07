package com.astro.stats.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UrlStatsResponse {
    private String slug;
    private String originalUrl;
    private long totalClicks;
    private LocalDateTime lastAccessed;
    private LocalDateTime createdAt;
}