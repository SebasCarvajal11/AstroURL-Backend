package com.astro.url.service;

import com.astro.config.RedisKeyManager; // Importar
import com.astro.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisKeyManager redisKeyManager; // Inyectar

    private static final int ANONYMOUS_URL_LIMIT = 7;
    private static final int REDIRECT_LIMIT_PER_MINUTE = 50;
    private static final int FORGOT_PASSWORD_LIMIT_PER_HOUR = 5;

    // ELIMINAR CONSTANTES
    // private static final String ANONYMOUS_KEY_PREFIX = "rate_limit:url_creation:ip:";
    // ...y las demás...

    public boolean isAllowedForAnonymous(String ipAddress) {
        // CORRECCIÓN: Usar el key manager
        String key = redisKeyManager.getAnonymousUrlRateLimitKey(ipAddress);
        return isAllowed(key, ANONYMOUS_URL_LIMIT, Duration.ofDays(1));
    }

    public boolean isAllowedForUser(User user) {
        // CORRECCIÓN: Usar el key manager
        String key = redisKeyManager.getAuthenticatedUserUrlRateLimitKey(user.getId());
        int limit = user.getPlan().getUrlLimitPerDay();
        if (limit < 0) return true;
        return isAllowed(key, limit, Duration.ofDays(1));
    }

    public boolean isRedirectAllowed(String ipAddress) {
        // CORRECCIÓN: Usar el key manager
        String key = redisKeyManager.getRedirectRateLimitKey(ipAddress);
        return isAllowed(key, REDIRECT_LIMIT_PER_MINUTE, Duration.ofMinutes(1));
    }

    public boolean isForgotPasswordAllowed(String ipAddress) {
        // CORRECCIÓN: Usar el key manager
        String key = redisKeyManager.getForgotPasswordRateLimitKey(ipAddress);
        return isAllowed(key, FORGOT_PASSWORD_LIMIT_PER_HOUR, Duration.ofHours(1));
    }

    private boolean isAllowed(String key, int limit, Duration ttl) {
        Long currentCount = redisTemplate.opsForValue().increment(key);
        if (currentCount == null) return false;
        if (currentCount == 1) {
            redisTemplate.expire(key, ttl);
        }
        return currentCount <= limit;
    }
}