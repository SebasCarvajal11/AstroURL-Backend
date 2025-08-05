package com.astro.url.controller;

import com.astro.url.dto.UrlResponse;
import com.astro.url.dto.UrlShortenRequest;
import com.astro.url.service.UrlService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/url")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    @PostMapping
    public ResponseEntity<UrlResponse> shortenUrl(@Valid @RequestBody UrlShortenRequest request,
                                                  HttpServletRequest httpServletRequest,
                                                  Authentication authentication) {
        String requestUrl = httpServletRequest.getRequestURL().toString();
        UrlResponse response;

        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            response = urlService.shortenUrlForAuthenticatedUser(request, user, requestUrl);
        } else {
            String ipAddress = httpServletRequest.getRemoteAddr();
            response = urlService.shortenUrlForAnonymous(request, ipAddress, requestUrl);
        }

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
        Page<UrlResponse> urls = urlService.getUrlsForUser(user, pageable, requestUrl);
        return ResponseEntity.ok(urls);
    }
}