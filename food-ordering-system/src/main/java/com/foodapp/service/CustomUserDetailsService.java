package com.foodapp.service;

import com.foodapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security calls loadUserByUsername() whenever it needs to verify
 * credentials (login) or rebuild the authenticated principal from a JWT
 * (on every subsequent request, via JwtAuthFilter).
 */
@Service
@RequiredArgsConstructor   // Lombok generates a constructor for all final fields -> enables constructor injection
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + email));
    }
}
