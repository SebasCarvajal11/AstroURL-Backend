package com.astro.auth.token;

import com.astro.config.RedisKeyManager; // Importar
import com.astro.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisKeyManager redisKeyManager; // Inyectar

    @Value("${jwt.expiration.access-ms}")
    private long accessTokenExpirationMs;

    public void revokeRefreshToken(User user) {
        // CORRECCIÓN: Usar el key manager
        String redisKey = redisKeyManager.getRefreshTokenKey(user.getId());
        redisTemplate.delete(redisKey);
    }

    public void blacklistUserTokens(User user) {
        // CORRECCIÓN: Usar el key manager
        String redisKey = redisKeyManager.getTokenBlacklistKey(user.getId());
        redisTemplate.opsForValue().set(redisKey, "revoked", accessTokenExpirationMs, TimeUnit.MILLISECONDS);
    }

    public boolean areTokensBlacklisted(User user) {
        // CORRECCIÓN: Usar el key manager
        String redisKey = redisKeyManager.getTokenBlacklistKey(user.getId());
        return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
    }
}