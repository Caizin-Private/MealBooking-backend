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
@Schema(description = "Request for booking a meal for a single day")
public class SingleMealBookingRequestDTO {

    @NotNull(message = "Date is required")
    @Schema(
            description = "Date for which meal should be booked",
            example = "2026-01-25",
            format = "date"
    )
    private LocalDate date;
}
