package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Schema(description = "Request object for updating cutoff configuration")
public class CutoffConfigRequest {

    @NotNull
    @Schema(
            description = "Cutoff time for meal bookings (24-hour format)",
            example = "22:00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalTime cutoffTime;
}
