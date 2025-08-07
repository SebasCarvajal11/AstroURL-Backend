package com.astro.auth.handler;

import com.astro.auth.dto.ForgotPasswordRequest;
import com.astro.auth.dto.ResetPasswordRequest;
import com.astro.shared.exceptions.InvalidTokenException;
import com.astro.shared.service.EmailService;
import com.astro.user.model.User;
import com.astro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class PasswordResetHandler {

    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${astrourl.frontend.reset-password-url}")
    private String resetPasswordUrlBase;

    private static final String RESET_TOKEN_PREFIX = "user:resetToken:";

    public void handleForgotPassword(ForgotPasswordRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String token = generateAndStoreResetToken(user);
            emailService.sendPasswordResetEmail(user, token, resetPasswordUrlBase);
        }
    }

    public void handleResetPassword(ResetPasswordRequest request) {
        validatePasswordConfirmation(request);
        String userId = validateAndRetrieveUserIdFromToken(request.getToken());
        User user = findUserById(userId);
        updateUserPassword(user, request.getNewPassword());
        invalidateResetToken(request.getToken());
    }

    private String generateAndStoreResetToken(User user) {
        String token = UUID.randomUUID().toString();
        String redisKey = RESET_TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(redisKey, user.getId().toString(), 15, TimeUnit.MINUTES);
        return token;
    }

    private void validatePasswordConfirmation(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
    }

    private String validateAndRetrieveUserIdFromToken(String token) {
        String redisKey = RESET_TOKEN_PREFIX + token;
        String userId = redisTemplate.opsForValue().get(redisKey);
        if (userId == null) {
            throw new InvalidTokenException("Invalid or expired password reset token.");
        }
        return userId;
    }

    private User findUserById(String userId) {
        return userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new InvalidTokenException("User not found for the given reset token."));
    }

    private void updateUserPassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private void invalidateResetToken(String token) {
        redisTemplate.delete(RESET_TOKEN_PREFIX + token);
    }
}