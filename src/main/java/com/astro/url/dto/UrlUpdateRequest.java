package com.astro.url.dto;

import com.astro.shared.annotations.ValidSlug;
import com.astro.shared.annotations.ValidUrl;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UrlUpdateRequest {

    @ValidUrl(message = "Must be a valid URL format.")
    private String originalUrl;

    @ValidSlug
    private String slug;

    private LocalDateTime expirationDate;

    private String password;
}