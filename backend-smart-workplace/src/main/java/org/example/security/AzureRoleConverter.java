package org.example.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public final class AzureRoleConverter {

    private AzureRoleConverter() {
    }

    public static Converter<Jwt, ? extends AbstractAuthenticationToken> converter() {
        return jwt -> new JwtAuthenticationToken(jwt, extractAuthorities(jwt), getPrincipalName(jwt));
    }

    private static Collection<? extends GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<String> roles = Set.copyOf(jwt.getClaimAsStringList("roles") == null
                ? List.of()
                : jwt.getClaimAsStringList("roles"));

        Set<String> scopes = Set.copyOf(jwt.getClaimAsStringList("scp") == null
                ? List.of()
                : jwt.getClaimAsStringList("scp"));

        return Stream.concat(
                        roles.stream().map(role -> "ROLE_" + role.toUpperCase(Locale.ROOT)),
                        scopes.stream().map(scope -> "SCOPE_" + scope)
                )
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String getPrincipalName(Jwt jwt) {
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            return preferredUsername;
        }
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            return email;
        }
        return jwt.getSubject();
    }
}

