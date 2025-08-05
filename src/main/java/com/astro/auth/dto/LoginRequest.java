package com.astro.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Login identifier (username or email) is required")
    private String loginIdentifier;

    @NotBlank(message = "Password is required")
    private String password;
}