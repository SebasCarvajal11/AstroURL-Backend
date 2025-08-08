package com.astro.url.controller;

import com.astro.shared.exceptions.RateLimitExceededException;
import com.astro.shared.utils.HttpServletRequestUtils;
import com.astro.url.retrieval.UrlRetrievalService;
import com.astro.url.service.RateLimitingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final UrlRetrievalService urlRetrievalService;
    private final RateLimitingService rateLimitingService;
    private final HttpServletRequestUtils requestUtils;

    @GetMapping("/r/{slug}")
    public ResponseEntity<Void> redirect(@PathVariable String slug, HttpServletRequest request) {
        String ipAddress = requestUtils.getClientIpAddress(request);
        if (!rateLimitingService.isRedirectAllowed(ipAddress)) {
            throw new RateLimitExceededException("Redirect rate limit exceeded.");
        }

        String userAgent = request.getHeader("User-Agent");
        String originalUrl = urlRetrievalService.getOriginalUrlAndLogClick(slug, ipAddress, userAgent);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(originalUrl));
        return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
    }
}