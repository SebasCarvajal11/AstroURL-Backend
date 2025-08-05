package com.astro.url.dto;

import com.astro.shared.annotations.ValidUrl;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UrlShortenRequest {
    @NotBlank(message = "Original URL is required.")
    @ValidUrl(message = "Must be a valid URL format (e.g., http://example.com or https://example.com)")
    private String originalUrl;
}
