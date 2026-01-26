package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.entity.User;
import org.example.service.AzureOAuth2UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "APIs for managing users")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class UserController {

        private final AzureOAuth2UserService azureOAuth2UserService;

        @GetMapping("/me")
        @Operation(summary = "Get current authenticated user", description = "Returns details of the currently authenticated user. Syncs user to DB if not exists.")
        public ResponseEntity<User> getCurrentUser() {
                User user = azureOAuth2UserService.getOrCreateAuthenticatedUser();
                return ResponseEntity.ok(user);
        }
}
