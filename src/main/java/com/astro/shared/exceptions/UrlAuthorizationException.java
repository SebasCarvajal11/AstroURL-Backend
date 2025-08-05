package com.astro.shared.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UrlAuthorizationException extends RuntimeException {
    public UrlAuthorizationException(String message) {
        super(message);
    }
}