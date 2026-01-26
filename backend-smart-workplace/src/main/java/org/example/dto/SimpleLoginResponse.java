package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleLoginResponse {
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        // Changed to String as per entity usage in other DTOs usually, checking entity
        // User.java... Entity id is Long.
        // Wait, User.java id is Long. Response builder sets id(user.getId()).
        // Let's use Object or Long.
        // Actually, user.getId() is Long (from User.java).
        // I will use Long for ID.
        private Long id;
        private String email;
        private String name;
    }
}
