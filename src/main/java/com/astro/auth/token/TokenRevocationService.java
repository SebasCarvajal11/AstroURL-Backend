package com.astro.auth.token;

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

    @Value("${jwt.expiration.access-ms}")
    private long accessTokenExpirationMs;

    private static final String REFRESH_TOKEN_KEY_PREFIX = "user:refreshToken:";
    private static final String TOKEN_BLACKLIST_KEY_PREFIX = "user:tokenBlacklist:";

    public void revokeRefreshToken(User user) {
        String redisKey = REFRESH_TOKEN_KEY_PREFIX + user.getId();
        redisTemplate.delete(redisKey);
    }

    public void blacklistUserTokens(User user) {
        String redisKey = TOKEN_BLACKLIST_KEY_PREFIX + user.getId();
        redisTemplate.opsForValue().set(redisKey, "revoked", accessTokenExpirationMs, TimeUnit.MILLISECONDS);
    }

    public boolean areTokensBlacklisted(User user) {
        String redisKey = TOKEN_BLACKLIST_KEY_PREFIX + user.getId();
        return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
    }
}