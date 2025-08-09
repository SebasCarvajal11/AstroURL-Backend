package com.astro.url.protection;

import com.astro.config.RedisKeyManager; // Importar
import com.astro.shared.exceptions.RateLimitExceededException;
import com.astro.shared.exceptions.UrlAuthorizationException;
import com.astro.url.model.Url;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class UrlProtectionService {

    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisKeyManager redisKeyManager; // Inyectar

    private static final int MAX_ATTEMPTS = 5;
    // ELIMINAR: private static final String ATTEMPTS_KEY_PREFIX = "rate_limit:url_password:";

    public void checkProtectionStatus(Url url, String providedPassword) {
        if (!isPasswordProtected(url)) {
            return;
        }
        if (!StringUtils.hasText(providedPassword)) {
            throw new UrlAuthorizationException("Password is required for this URL.");
        }
        if (isBruteForceLocked(url.getSlug())) {
            throw new RateLimitExceededException("Too many failed password attempts. Please try again later.");
        }
        if (!isPasswordCorrect(providedPassword, url.getPassword())) {
            incrementFailedAttempts(url.getSlug());
            throw new UrlAuthorizationException("Incorrect password.");
        }
    }

    private boolean isPasswordProtected(Url url) {
        return StringUtils.hasText(url.getPassword());
    }

    private boolean isPasswordCorrect(String raw, String hashed) {
        return passwordEncoder.matches(raw, hashed);
    }

    private void incrementFailedAttempts(String slug) {
        // CORRECCIÓN: Usar el key manager
        String key = redisKeyManager.getUrlPasswordAttemptsKey(slug);
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(key, Duration.ofMinutes(15));
        }
    }

    private boolean isBruteForceLocked(String slug) {
        // CORRECCIÓN: Usar el key manager
        String key = redisKeyManager.getUrlPasswordAttemptsKey(slug);
        String attempts = redisTemplate.opsForValue().get(key);
        return attempts != null && Integer.parseInt(attempts) >= MAX_ATTEMPTS;
    }
}