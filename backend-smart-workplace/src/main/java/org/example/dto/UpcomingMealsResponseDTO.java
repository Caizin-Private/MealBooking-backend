package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response for user's upcoming meals")
public class UpcomingMealsResponseDTO {

    @Schema(description = "Indicates if the request was successful")
    private boolean success;

    @Schema(description = "Message describing the result")
    private String message;

    @Schema(description = "List of user's upcoming meal bookings")
    private List<MealBookingInfo> bookings;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Information about a meal booking")
    public static class MealBookingInfo {
        @Schema(description = "Date of the meal booking")
        private LocalDate bookingDate;
    }

    public static UpcomingMealsResponseDTO success(String message, List<MealBookingInfo> bookings) {
        return new UpcomingMealsResponseDTO(true, message, bookings);
    }

    public static UpcomingMealsResponseDTO failure(String message) {
        return new UpcomingMealsResponseDTO(false, message, null);
    }
}
