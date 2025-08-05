package com.astro.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileResponse {
    private String username;
    private String email;
    private String planName;
}