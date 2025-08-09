package com.astro.auth.controller;

import com.astro.auth.dto.*;
import com.astro.auth.service.AuthService;
import com.astro.shared.dto.ApiResponse;
import com.astro.shared.exceptions.RateLimitExceededException;
import com.astro.shared.utils.HttpServletRequestUtils;
import com.astro.url.service.RateLimitingService;
import com.astro.user.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RateLimitingService rateLimitingService;
    private final HttpServletRequestUtils requestUtils;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.registerUser(registerRequest);
        return new ResponseEntity<>(new ApiResponse(true, "User registered successfully!"), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse loginResponse = authService.loginUser(loginRequest);
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        String ipAddress = requestUtils.getClientIpAddress(httpRequest);
        if (!rateLimitingService.isForgotPasswordAllowed(ipAddress)) {
            throw new RateLimitExceededException("You have made too many password reset requests. Please try again later.");
        }
        authService.forgotPassword(request);
        String message = "If an account with that email address exists, a password reset link has been sent.";
        return ResponseEntity.ok(new ApiResponse(true, message));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(new ApiResponse(true, "Your password has been reset successfully."));
    }

    /**
     * CORRECCIÓN: El método ya no recibe un cuerpo de petición con la contraseña.
     * La autorización se basa únicamente en el token JWT del usuario autenticado.
     */
    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logoutAll(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        authService.logoutAll(currentUser);
        return ResponseEntity.noContent().build();
    }
}