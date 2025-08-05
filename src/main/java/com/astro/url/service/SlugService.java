package com.astro.url.service;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;

@Service
public class SlugService {

    private static final String ALPHANUMERIC_CHARS = "abcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom random = new SecureRandom();

    public String generateRandomSlug(int length) {
        StringBuilder slug = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            slug.append(ALPHANUMERIC_CHARS.charAt(random.nextInt(ALPHANUMERIC_CHARS.length())));
        }
        return slug.toString();
    }
}