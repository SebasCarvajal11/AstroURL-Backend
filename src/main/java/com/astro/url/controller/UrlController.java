package com.astro.url.controller;

import com.astro.url.dto.UrlShortenRequest;
import com.astro.url.dto.UrlShortenResponse;
import com.astro.url.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<UrlShortenResponse> shortenUrl(@Valid @RequestBody UrlShortenRequest request, HttpServletRequest httpServletRequest) {
        String ipAddress = httpServletRequest.getRemoteAddr();
        String requestUrl = httpServletRequest.getRequestURL().toString();

        UrlShortenResponse response = urlService.shortenUrlForAnonymous(request, ipAddress, requestUrl);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}