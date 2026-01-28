package org.example.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityUserResolver {

    private final UserRepository userRepository;

    @Transactional
    public User resolveUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            log.warn("⚠️ [RESOLVER] No valid JWT Authentication found in context");
            throw new IllegalStateException("User not authenticated");
        }

        String objectId = extractOid(jwt);
        String email = extractEmail(jwt);
        String name = extractName(jwt);

        log.debug("🔎 [RESOLVER] Resolving User: oid={}, email={}", objectId, email);

        return userRepository.findByExternalId(objectId)
                .map(existing -> {
                    existing.setName(name);
                    return existing;
                })
                .orElseGet(() -> {
                    log.info("🆕 [RESOLVER] Auto-provisioning new user: {}", email);
                    User newUser = User.builder()
                            .externalId(objectId)
                            .email(email)
                            .name(name)
                            .role(org.example.entity.Role.USER)
                            .createdAt(LocalDateTime.now())
                            .lastLoginAt(LocalDateTime.now())
                            .build();
                    return userRepository.save(newUser);
                });
    }

    private String extractOid(Jwt jwt) {
        String oid = jwt.getClaim("oid");
        return (oid != null) ? oid : jwt.getSubject();
    }

    private String extractEmail(Jwt jwt) {
        if (jwt.hasClaim("email"))
            return jwt.getClaim("email");
        if (jwt.hasClaim("preferred_username"))
            return jwt.getClaim("preferred_username");
        if (jwt.hasClaim("upn"))
            return jwt.getClaim("upn");
        return "unknown-" + extractOid(jwt);
    }

    private String extractName(Jwt jwt) {
        if (jwt.hasClaim("name"))
            return jwt.getClaim("name");
        return extractEmail(jwt);
    }
}
