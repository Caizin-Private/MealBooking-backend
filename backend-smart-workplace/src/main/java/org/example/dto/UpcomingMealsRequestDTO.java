package org.example.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class UpcomingMealsRequestDTO {
    // No fields needed - userId comes from X-USER-ID header
}
