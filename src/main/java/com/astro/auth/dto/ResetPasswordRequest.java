package com.astro.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Token is required.")
    private String token;

    @NotBlank(message = "New password is required.")
    @Size(min = 7, message = "New password must be at least 7 characters long.")
    private String newPassword;

    @NotBlank(message = "Password confirmation is required.")
    private String confirmPassword;
}