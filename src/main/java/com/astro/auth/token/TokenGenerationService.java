package com.astro.auth.token;

import com.astro.auth.dto.LoginResponse;
import com.astro.auth.security.JwtTokenProvider;
import com.astro.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenGenerationService {

    private final JwtTokenProvider tokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    public LoginResponse generateAndStoreTokens(Authentication authentication) {
        String accessToken = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);
        storeRefreshToken(authentication, refreshToken);
        return new LoginResponse(accessToken, refreshToken);
    }

    private void storeRefreshToken(Authentication authentication, String refreshToken) {
        User userPrincipal = (User) authentication.getPrincipal();
        String redisKey = "user:refreshToken:" + userPrincipal.getId();
        redisTemplate.opsForValue().set(redisKey, refreshToken, 7, TimeUnit.DAYS);
    }
}