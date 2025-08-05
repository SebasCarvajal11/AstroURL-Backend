package com.astro.auth.service;

import com.astro.auth.dto.RegisterRequest;
import com.astro.user.model.User;
import com.astro.user.plan.model.Plan;
import com.astro.user.plan.repository.PlanRepository;
import com.astro.user.repository.UserRepository;
import com.astro.shared.exceptions.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void registerUser(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException("Username '" + request.getUsername() + "' is already taken.");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email '" + request.getEmail() + "' is already in use.");
        }

        Plan polarisPlan = planRepository.findByName("Polaris")
                .orElseThrow(() -> new IllegalStateException("Default plan 'Polaris' not found in database."));

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPlan(polarisPlan);

        userRepository.save(user);
    }
}