package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response for meal booking operations")
public class MealBookingResponseDTO {

    @Schema(description = "Indicates if the booking was successful")
    private boolean success;

    @Schema(description = "Message describing the result")
    private String message;

    @Schema(description = "ID of the created booking")
    private Long bookingId;

    @Schema(description = "Date of the booked meal")
    private String bookingDate;

    public static MealBookingResponseDTO success(String message, Long bookingId, String bookingDate) {
        return new MealBookingResponseDTO(true, message, bookingId, bookingDate);
    }

    public static MealBookingResponseDTO failure(String message) {
        return new MealBookingResponseDTO(false, message, null, null);
    }
}
