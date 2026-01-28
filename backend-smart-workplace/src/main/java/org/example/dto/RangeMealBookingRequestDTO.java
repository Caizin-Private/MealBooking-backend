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
@Schema(description = "Request for booking meals for a date range")
public class RangeMealBookingRequestDTO {

    @NotNull(message = "Start date is required")
    @Schema(
            description = "Start date for meal booking range",
            example = "2026-01-25",
            format = "date"
    )
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Schema(
            description = "End date for meal booking range",
            example = "2026-01-27",
            format = "date"
    )
    private LocalDate endDate;
}
