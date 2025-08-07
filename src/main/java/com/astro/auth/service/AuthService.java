package com.astro.auth.service;

import com.astro.auth.dto.*;
import com.astro.auth.handler.LoginHandler;
import com.astro.auth.handler.PasswordResetHandler;
import com.astro.auth.handler.RegistrationHandler;
import com.astro.auth.handler.TokenHandler;
import com.astro.auth.service.validation.AuthValidatorService;
import com.astro.user.model.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final RegistrationHandler registrationHandler;
    private final LoginHandler loginHandler;
    private final PasswordResetHandler passwordResetHandler;
    private final TokenHandler tokenHandler;
    private final AuthValidatorService authValidator;

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Transactional
    public void registerUser(RegisterRequest request) {
        registrationHandler.handleRegistration(request);
    }

    public LoginResponse loginUser(LoginRequest loginRequest) {
        return loginHandler.handleLogin(loginRequest);
    }

    @Transactional(readOnly = true)
    public void forgotPassword(ForgotPasswordRequest request) {
        passwordResetHandler.handleForgotPassword(request);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        passwordResetHandler.handleResetPassword(request);
    }

    public LoginResponse refreshAccessToken(String refreshToken) {
        return tokenHandler.handleTokenRefresh(refreshToken);
    }

    public void logoutAll(User currentUser, String providedPassword) {
        if (!authValidator.isPasswordCorrect(currentUser, providedPassword)) {
            throw new IllegalArgumentException("Incorrect password provided.");
        }
        tokenHandler.handleLogoutAll(currentUser);
        logger.info("User {} successfully logged out from all devices.", currentUser.getUsername());
    }
}