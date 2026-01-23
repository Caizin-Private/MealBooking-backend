package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "Request object for cancelling a meal booking")
public class MealCancelRequestDTO {

    @NotNull
    @Schema(
            description = "Date of the meal booking to cancel",
            example = "2026-01-25",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDate date;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
