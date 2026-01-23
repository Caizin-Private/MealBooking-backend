package org.example.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User entity representing an employee in the system")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier for the user", example = "1")
    private Long id;

    @Column(nullable = false)
    @Schema(description = "Full name of the user", example = "John Doe")
    private String name;

    @Column(nullable = false, unique = true)
    @Schema(description = "Email address of the user (unique)", example = "john.doe@company.com")
    private String email;

//    @Column(nullable = false)
//    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "Role of the user in the system", allowableValues = {"USER", "ADMIN"})
    private Role role;

    @Column(nullable = false)
    @Schema(description = "Timestamp when the user account was created", example = "2026-01-22T10:30:00")
    private LocalDateTime createdAt;
}