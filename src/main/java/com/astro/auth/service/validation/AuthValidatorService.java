package com.astro.auth.service.validation;

import com.astro.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthValidatorService {

    private final PasswordEncoder passwordEncoder;

    public boolean isPasswordCorrect(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }
}