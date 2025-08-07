package com.astro.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordConfirmationRequest {

    @NotBlank(message = "Current password is required for confirmation.")
    private String currentPassword;
}