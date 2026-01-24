package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to get meals for a specific user")
public class UserIdRequestDTO {

    @NotNull(message = "User ID is required")
    @Schema(description = "ID of the user to retrieve meals for", example = "123")
    private Long userId;
}
