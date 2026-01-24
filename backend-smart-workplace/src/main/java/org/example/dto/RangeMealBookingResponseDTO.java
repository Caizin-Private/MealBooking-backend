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

    @Schema(description = "Indicates if the booking was successful")
    private boolean success;

    @Schema(description = "Message describing the result")
    private String message;

    @Schema(description = "List of successfully booked dates")
    private List<String> bookedDates;

    @Schema(description = "List of failed bookings with reasons")
    private List<String> failedBookings;

    public static RangeMealBookingResponseDTO success(String message, List<String> bookedDates) {
        return new RangeMealBookingResponseDTO(true, message, bookedDates, null);
    }

    public static RangeMealBookingResponseDTO failure(String message, List<String> failedBookings) {
        return new RangeMealBookingResponseDTO(false, message, null, failedBookings);
    }
}
