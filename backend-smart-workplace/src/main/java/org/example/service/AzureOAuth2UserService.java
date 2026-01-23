package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AzureOAuth2UserService {

    private final UserRepository userRepository;

    public User getOrCreateAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("User not authenticated or invalid token type");
        }

        String externalId = extractExternalId(jwt);
        String email = extractEmail(jwt);
        String name = extractName(jwt);

        log.debug("Processing Azure OAuth user: externalId={}, email={}, name={}", externalId, email, name);

        Optional<User> existingUser = userRepository.findByExternalId(externalId);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setLastLoginAt(LocalDateTime.now());
            log.info("Updated last login for existing user: {}", user.getEmail());
            return userRepository.save(user);
        } else {
            User newUser = User.builder()
                    .externalId(externalId)
                    .email(email)
                    .name(name)
                    .role(Role.USER)
                    .createdAt(LocalDateTime.now())
                    .lastLoginAt(LocalDateTime.now())
                    .build();

            User savedUser = userRepository.save(newUser);
            log.info("Created new user from Azure OAuth: {}", savedUser.getEmail());
            return savedUser;
        }
    }

    private String extractExternalId(Jwt jwt) {
        return jwt.getClaim("oid") != null ? jwt.getClaim("oid") : jwt.getSubject();
    }

    private String extractEmail(Jwt jwt) {
        return jwt.getClaim("email") != null ? jwt.getClaim("email") : jwt.getClaim("upn");
    }

    private String extractName(Jwt jwt) {
        return jwt.getClaim("name") != null ? jwt.getClaim("name") : jwt.getClaim("preferred_username");
    }

    public User getCurrentUser() {
        return getOrCreateAuthenticatedUser();
    }
}
