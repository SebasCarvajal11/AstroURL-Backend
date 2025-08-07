package com.astro.url.mapper;

import com.astro.url.dto.UrlResponse;
import com.astro.url.model.Url;
import org.springframework.stereotype.Component;

@Component
public class UrlMapper {

    public UrlResponse toUrlResponse(Url url, String baseUrl) {
        return UrlResponse.builder()
                .slug(url.getSlug())
                .originalUrl(url.getOriginalUrl())
                .shortUrl(baseUrl + "/r/" + url.getSlug())
                .expirationDate(url.getExpirationDate())
                .createdAt(url.getCreatedAt())
                .clickCount(url.getClickCount())
                .build();
    }
}