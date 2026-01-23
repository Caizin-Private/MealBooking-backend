package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "Request object for booking meals for a date range")
public class MealBookingRequestDTO {

    @NotNull
    @Schema(
            description = "Start date for meal booking",
            example = "2026-01-25",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    LocalDate startDate;

    @NotNull
    @Schema(
            description = "End date for meal booking",
            example = "2026-01-25",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    LocalDate endDate;

    @Schema(
            description = "Current latitude of the user for geofence validation",
            example = "18.5204",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    double latitude;

    @Schema(
            description = "Current longitude of the user for geofence validation",
            example = "73.8567",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    double longitude;
}
