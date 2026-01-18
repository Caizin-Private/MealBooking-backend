package org.example.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class MealBookingRequestDTO {

    private List<LocalDate> bookingDates;

    private Double latitude;

    private Double longitude;
}
