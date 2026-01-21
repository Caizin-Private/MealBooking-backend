package org.example.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Ensures the Azure AD access token is issued for the configured client (audience).
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String allowedAudience;

    public AudienceValidator(String allowedAudience) {
        this.allowedAudience = allowedAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        List<String> audiences = token.getAudience();
        if (!CollectionUtils.isEmpty(audiences) && audiences.contains(allowedAudience)) {
            return OAuth2TokenValidatorResult.success();
        }
        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "The required audience is missing",
                null
        );
        return OAuth2TokenValidatorResult.failure(error);
    }
}

