package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to cancel a meal for a specific user")
public class CancelMealRequestDTO {

    @NotNull(message = "User ID is required")
    @Schema(description = "ID of the user to cancel meal for", example = "123")
    private Long userId;

    @NotNull(message = "Booking date is required")
    @Schema(description = "Date of the meal booking to cancel", example = "2026-01-25")
    private LocalDate bookingDate;
}
