package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.SimpleLoginRequest;
import org.example.dto.SimpleLoginResponse;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SimpleAuthController {

    private final UserRepository userRepository;

    @PostMapping("/simple-login")
    public ResponseEntity<SimpleLoginResponse> simpleLogin(@RequestBody SimpleLoginRequest request) {
        log.info("Simple login request for email: {}", request.getEmail());

        try {
            // Find or create user
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setEmail(request.getEmail());
                        newUser.setName(request.getName());
                        newUser.setExternalId("test-oid-" + System.currentTimeMillis()); // Unique Azure OID for test
                        newUser.setRole(org.example.entity.Role.USER);
                        newUser.setCreatedAt(LocalDateTime.now());
                        newUser.setLastLoginAt(LocalDateTime.now());
                        User saved = userRepository.save(newUser);
                        log.info("Created new user: {} (ID: {})", saved.getEmail(), saved.getId());
                        return saved;
                    });

            // Update last login
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("Login successful for user: {} (ID: {})", user.getEmail(), user.getId());

            return ResponseEntity.ok(SimpleLoginResponse.builder()
                    .user(SimpleLoginResponse.UserInfo.builder()
                            .id(user.getId())
                            .email(user.getEmail())
                            .name(user.getName())
                            .build())
                    .build());

        } catch (Exception e) {
            log.error("Login failed for email: {}", request.getEmail(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
