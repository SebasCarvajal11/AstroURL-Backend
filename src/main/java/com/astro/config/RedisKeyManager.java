package com.astro.config;

import org.springframework.stereotype.Component;

/**
 * Componente centralizado para gestionar la generación de todas las claves de Redis.
 * Esto evita el uso de "magic strings" dispersas por la aplicación y proporciona
 * un único punto de verdad para el esquema de claves de Redis.
 */
@Component
public class RedisKeyManager {

    // --- Claves de Autenticación y Sesión ---

    public String getRefreshTokenKey(Long userId) {
        return "user:refreshToken:" + userId;
    }

    public String getPasswordResetTokenKey(String token) {
        return "user:resetToken:" + token;
    }

    public String getTokenBlacklistKey(Long userId) {
        return "user:tokenBlacklist:" + userId;
    }

    // --- Claves de URL y Caché ---

    public String getUrlCacheKey(String slug) {
        return "url:slug:" + slug.toLowerCase();
    }

    // --- Claves de Limitación de Tasa (Rate Limiting) ---

    public String getAnonymousUrlRateLimitKey(String ipAddress) {
        return "rate_limit:url_creation:ip:" + ipAddress;
    }

    public String getAuthenticatedUserUrlRateLimitKey(Long userId) {
        return "rate_limit:url_creation:user:" + userId;
    }

    public String getRedirectRateLimitKey(String ipAddress) {
        return "rate_limit:redirect:ip:" + ipAddress;
    }

    public String getForgotPasswordRateLimitKey(String ipAddress) {
        return "rate_limit:forgot_password:ip:" + ipAddress;
    }

    public String getUrlPasswordAttemptsKey(String slug) {
        return "rate_limit:url_password:" + slug;
    }

    // --- Claves de Procesos por Lotes (Batch Jobs) ---

    public String getLastProcessedTimestampKey() {
        return "stats:lastProcessedClickTimestamp";
    }
}