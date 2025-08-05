package com.astro.url.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UrlShortenResponse {
    private String slug;
    private String shortUrl;
}