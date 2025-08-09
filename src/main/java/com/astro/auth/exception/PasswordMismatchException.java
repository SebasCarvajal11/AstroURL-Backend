package com.astro.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción específica de negocio que se lanza cuando una contraseña
 * y su confirmación no coinciden durante operaciones como el registro
 * o el reseteo de contraseña.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PasswordMismatchException extends RuntimeException {
    public PasswordMismatchException(String message) {
        super(message);
    }
}