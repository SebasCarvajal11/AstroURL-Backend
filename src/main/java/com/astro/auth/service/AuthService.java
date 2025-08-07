package com.astro.auth.service;

import com.astro.auth.dto.ForgotPasswordRequest;
import com.astro.auth.dto.LoginRequest;
import com.astro.auth.dto.LoginResponse;
import com.astro.auth.dto.RegisterRequest;
import com.astro.auth.security.JwtTokenProvider;
import com.astro.shared.exceptions.TokenRefreshException;
import com.astro.shared.exceptions.UserAlreadyExistsException;
import com.astro.shared.service.EmailService;
import com.astro.user.model.User;
import com.astro.user.plan.model.Plan;
import com.astro.user.plan.repository.PlanRepository;
import com.astro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final EmailService emailService;

    @Value("${astrourl.frontend.reset-password-url}")
    private String resetPasswordUrlBase;

    private static final String RESET_TOKEN_PREFIX = "user:resetToken:";

    @Transactional
    public void registerUser(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException("Username '" + request.getUsername() + "' is already taken.");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email '" + request.getEmail() + "' is already in use.");
        }
        Plan polarisPlan = planRepository.findByName("Polaris")
                .orElseThrow(() -> new IllegalStateException("Default plan 'Polaris' not found in database."));
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPlan(polarisPlan);
        userRepository.save(user);
    }

    public LoginResponse loginUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getLoginIdentifier(),
                        loginRequest.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return generateAndStoreTokens(authentication);
    }

    @Transactional(readOnly = true)
    public void forgotPassword(ForgotPasswordRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String token = UUID.randomUUID().toString();
            String redisKey = RESET_TOKEN_PREFIX + token;

            redisTemplate.opsForValue().set(redisKey, user.getId().toString(), 15, TimeUnit.MINUTES);

            emailService.sendPasswordResetEmail(user.getEmail(), token, resetPasswordUrlBase);
        }
    }

    public LoginResponse refreshAccessToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new TokenRefreshException(refreshToken, "Refresh token is invalid or expired!");
        }
        String username = tokenProvider.getUsernameFromJWT(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User associated with token not found."));
        String redisKey = "user:refreshToken:" + user.getId();
        String storedToken = redisTemplate.opsForValue().get(redisKey);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new TokenRefreshException(refreshToken, "Refresh token is not in store or does not match. Please log in again.");
        }
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        return generateAndStoreTokens(authentication);
    }

    private LoginResponse generateAndStoreTokens(Authentication authentication) {
        String accessToken = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);
        User userPrincipal = (User) authentication.getPrincipal();
        String redisKey = "user:refreshToken:" + userPrincipal.getId();
        redisTemplate.opsForValue().set(redisKey, refreshToken, 7, TimeUnit.DAYS);
        return new LoginResponse(accessToken, refreshToken);
    }
}