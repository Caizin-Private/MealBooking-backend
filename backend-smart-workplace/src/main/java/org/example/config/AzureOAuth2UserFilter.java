package org.example.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.AzureOAuth2UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class AzureOAuth2UserFilter extends OncePerRequestFilter {

    private final AzureOAuth2UserService azureOAuth2UserService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            try {
                azureOAuth2UserService.getOrCreateAuthenticatedUser();
                log.debug("Successfully processed Azure OAuth user for request: {}", request.getRequestURI());
            } catch (Exception e) {
                log.error("Error processing Azure OAuth user", e);
            }
        }

        filterChain.doFilter(request, response);
    }
}
