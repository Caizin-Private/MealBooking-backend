package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response for range meal booking operations")
public class RangeMealBookingResponseDTO {

    @Schema(description = "Message describing the result")
    private String message;

    @Schema(description = "List of successfully booked dates")
    private List<String> bookedDates;

    public static RangeMealBookingResponseDTO success(String message, List<String> bookedDates) {
        return new RangeMealBookingResponseDTO(message, bookedDates);
    }

    public static RangeMealBookingResponseDTO failure(String message) {
        return new RangeMealBookingResponseDTO(message, null);
    }
}
