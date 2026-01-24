package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for managing user accounts and profiles")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserRepository userRepository;

    @PostMapping
    @Operation(
            summary = "Create a new user",
            description = "Create a new user account in the system. The user will be assigned the USER role by default."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = User.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid user data",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = "Email already exists")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid credentials"
            )
    })
    public User createUser(
            @Parameter(
                    description = "User details to create",
                    required = true,
                    schema = @Schema(implementation = User.class)
            )
            @RequestBody User user
    ) {
        user.setCreatedAt(LocalDateTime.now());
        user.setRole(Role.USER);
        return userRepository.save(user);
    }

    @GetMapping("/{email}")
    @Operation(
            summary = "Get user by email",
            description = "Retrieve user details using their email address. Returns complete user profile information."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User found successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = User.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = "User not found")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid credentials"
            )
    })
    public User getUserByEmail(
            @Parameter(
                    description = "Email address of the user to retrieve",
                    required = true,
                    example = "user@company.com"
            )
            @PathVariable String email
    ) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
