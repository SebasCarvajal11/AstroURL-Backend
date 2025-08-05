package com.astro.url.service;

import com.astro.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final int ANONYMOUS_URL_LIMIT = 7;
    private static final String ANONYMOUS_KEY_PREFIX = "rate_limit:url_creation:ip:";
    private static final String AUTH_KEY_PREFIX = "rate_limit:url_creation:user:";

    public boolean isAllowedForAnonymous(String ipAddress) {
        String key = ANONYMOUS_KEY_PREFIX + ipAddress;
        return isAllowed(key, ANONYMOUS_URL_LIMIT);
    }

    public boolean isAllowedForUser(User user) {
        String key = AUTH_KEY_PREFIX + user.getId();
        int limit = user.getPlan().getUrlLimitPerDay();

        // A limit of -1 means unlimited
        if (limit < 0) {
            return true;
        }

        return isAllowed(key, limit);
    }

    private boolean isAllowed(String key, int limit) {
        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount == null) {
            // This should ideally not happen in a stable Redis environment
            return false;
        }

        if (currentCount == 1) {
            // First request for this key, set expiry to 24 hours (end of day UTC is better, but this is simpler)
            redisTemplate.expire(key, Duration.ofDays(1));
        }

        return currentCount <= limit;
    }
}