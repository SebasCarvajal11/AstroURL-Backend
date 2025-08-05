package com.astro.url.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final int ANONYMOUS_URL_LIMIT = 7;
    private static final String KEY_PREFIX = "rate_limit:url_creation:";

    public boolean isAllowed(String ipAddress) {
        String key = KEY_PREFIX + ipAddress;
        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount == null) {
            return false; // Should not happen
        }

        if (currentCount == 1) {
            // First request from this IP today, set expiry to 24 hours.
            redisTemplate.expire(key, Duration.ofDays(1));
        }

        return currentCount <= ANONYMOUS_URL_LIMIT;
    }
}