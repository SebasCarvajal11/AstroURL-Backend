package com.astro.url.controller;

import com.astro.url.creation.UrlCreationService;
import com.astro.url.dto.UrlResponse;
import com.astro.url.dto.UrlShortenRequest;
import com.astro.url.dto.UrlUpdateRequest;
import com.astro.url.retrieval.UrlRetrievalService;
import com.astro.url.update.UrlUpdateService;
import com.astro.url.validation.UrlValidationService;
import com.astro.user.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/url")
@RequiredArgsConstructor
public class UrlController {

    private final UrlCreationService urlCreationService;
    private final UrlRetrievalService urlRetrievalService;
    private final UrlValidationService urlValidationService;
    private final UrlUpdateService urlUpdateService;

    @PostMapping
    public ResponseEntity<UrlResponse> shortenUrl(@Valid @RequestBody UrlShortenRequest request,
                                                  HttpServletRequest httpServletRequest,
                                                  Authentication authentication) {
        User user = (authentication != null && authentication.isAuthenticated()) ? (User) authentication.getPrincipal() : null;
        String ipAddress = (user == null) ? httpServletRequest.getRemoteAddr() : null;
        String requestUrl = httpServletRequest.getRequestURL().toString();

        UrlResponse response = urlCreationService.createShortUrl(request, user, ipAddress, requestUrl);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<UrlResponse>> getUserUrls(
            @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpServletRequest,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        String requestUrl = httpServletRequest.getRequestURL().toString();
        Page<UrlResponse> urls = urlRetrievalService.getUrlsForUser(user, pageable, requestUrl);
        return ResponseEntity.ok(urls);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PLAN_SIRIO')")
    public ResponseEntity<UrlResponse> updateUrl(@PathVariable Long id,
                                                 @Valid @RequestBody UrlUpdateRequest request,
                                                 HttpServletRequest httpServletRequest,
                                                 Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String requestUrl = httpServletRequest.getRequestURL().toString();
        UrlResponse updatedUrl = urlUpdateService.updateUrl(id, request, user, requestUrl);
        return ResponseEntity.ok(updatedUrl);
    }

    @GetMapping("/validate-slug/{slug}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PLAN_SIRIO')")
    public ResponseEntity<Map<String, Boolean>> isSlugAvailable(@PathVariable String slug) {
        boolean isAvailable = urlValidationService.isSlugAvailable(slug);
        return ResponseEntity.ok(Map.of("available", isAvailable));
    }
}