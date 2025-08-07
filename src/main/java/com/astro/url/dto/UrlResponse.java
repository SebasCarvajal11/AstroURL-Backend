package com.astro.url.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UrlResponse {
    private String slug;
    private String originalUrl;
    private String shortUrl;
    private LocalDateTime expirationDate;
    private LocalDateTime createdAt;
    private long clickCount;
}