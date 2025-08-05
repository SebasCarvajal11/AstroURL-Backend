package com.astro.auth.service;

import com.astro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true) // readOnly transaction is efficient for find operations.
    public UserDetails loadUserByUsername(String loginIdentifier) throws UsernameNotFoundException {
        return userRepository.findByIdentifierWithPlan(loginIdentifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + loginIdentifier));
    }
}