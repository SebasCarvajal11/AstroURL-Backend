package com.astro.user.mapper;

import com.astro.user.dto.ProfileResponse;
import com.astro.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public ProfileResponse toProfileResponse(User user) {
        return ProfileResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .planName(user.getPlan().getName())
                .build();
    }
}