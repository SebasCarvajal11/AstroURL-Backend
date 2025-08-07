package com.astro.auth.handler;

import com.astro.auth.dto.LoginResponse;
import com.astro.auth.token.TokenGenerationService;
import com.astro.auth.token.TokenRevocationService;
import com.astro.auth.security.JwtTokenProvider;
import com.astro.shared.exceptions.TokenRefreshException;
import com.astro.user.model.User;
import com.astro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenHandler {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final TokenGenerationService tokenGenerationService;
    private final TokenRevocationService tokenRevocationService;
    private final RedisTemplate<String, String> redisTemplate;

    public LoginResponse handleTokenRefresh(String refreshToken) {
        validateRefreshToken(refreshToken);
        User user = findUserFromRefreshToken(refreshToken);
        validateStoredToken(user, refreshToken);
        Authentication authentication = createAuthentication(user);
        return tokenGenerationService.generateAndStoreTokens(authentication);
    }

    private void validateRefreshToken(String token) {
        if (!tokenProvider.validateToken(token)) {
            throw new TokenRefreshException(token, "Refresh token is invalid or expired!");
        }
    }

    private User findUserFromRefreshToken(String token) {
        String username = tokenProvider.getUsernameFromJWT(token);
        return userRepository.findByIdentifierWithPlan(username)
                .orElseThrow(() -> new UsernameNotFoundException("User associated with token not found."));
    }

    private void validateStoredToken(User user, String token) {
        String redisKey = "user:refreshToken:" + user.getId();
        String storedToken = redisTemplate.opsForValue().get(redisKey);
        if (storedToken == null || !storedToken.equals(token)) {
            throw new TokenRefreshException(token, "Refresh token is not in store or does not match. Please log in again.");
        }
    }

    private Authentication createAuthentication(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    public void handleLogoutAll(User user) {
        tokenRevocationService.revokeRefreshToken(user);
        tokenRevocationService.blacklistUserTokens(user);
    }
}