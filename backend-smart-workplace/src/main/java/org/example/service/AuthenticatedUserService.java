package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticatedUserService {

    private final UserRepository userRepository;

    /**
     * Loads the user from DB, creating/updating it from Azure AD claims if missing.
     */
    @Transactional
    public User loadOrCreateUser(Jwt jwt) {
        String email = extractEmail(jwt);
        if (!StringUtils.hasText(email)) {
            throw new AccessDeniedException("Email claim is required");
        }

        Role resolvedRole = resolveRole(jwt).orElse(Role.USER);

        User user = userRepository.findByEmail(email)
                .map(existing -> updateLoginMetadata(existing, resolvedRole))
                .orElseGet(() -> createUser(jwt, email, resolvedRole));

        return userRepository.save(user);
    }

    private User updateLoginMetadata(User user, Role resolvedRole) {
        user.setLastLoginAt(LocalDateTime.now());
        if (resolvedRole != null) {
            user.setRole(resolvedRole);
        }
        return user;
    }

    private User createUser(Jwt jwt, String email, Role role) {
        return User.builder()
                .email(email)
                .name(extractName(jwt))
                .role(role)
                .createdAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .authProvider("AZURE_AD")
                .providerUserId(extractProviderUserId(jwt))
                .pictureUrl(jwt.getClaimAsString("picture"))
                .build();
    }

    private String extractEmail(Jwt jwt) {
        List<String> candidateClaims = List.of("email", "preferred_username", "upn", "unique_name");
        for (String claim : candidateClaims) {
            String value = jwt.getClaimAsString(claim);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String extractName(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        if (StringUtils.hasText(name)) {
            return name;
        }

        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        String combined = (givenName == null ? "" : givenName) + " " + (familyName == null ? "" : familyName);
        if (StringUtils.hasText(combined.trim())) {
            return combined.trim();
        }

        return extractEmail(jwt);
    }

    private String extractProviderUserId(Jwt jwt) {
        String objectId = jwt.getClaimAsString("oid");
        if (StringUtils.hasText(objectId)) {
            return objectId;
        }
        return jwt.getSubject();
    }

    private Optional<Role> resolveRole(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            for (String role : roles) {
                try {
                    return Optional.of(Role.valueOf(role.toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                    // ignore unmapped roles
                }
            }
        }
        return Optional.of(Role.USER);
    }
}

