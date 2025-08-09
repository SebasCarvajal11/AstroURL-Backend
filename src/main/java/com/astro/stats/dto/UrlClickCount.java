package com.astro.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UrlClickCount {
    private Long urlId;
    private Long clickCount;
    private LocalDateTime maxTimestamp;
}