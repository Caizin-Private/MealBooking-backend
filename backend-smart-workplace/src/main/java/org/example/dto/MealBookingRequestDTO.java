package org.example.dto;

import lombok.Getter;
import lombok.Setter;
import org.antlr.v4.runtime.misc.NotNull;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class MealBookingRequestDTO {

    @NotNull
    LocalDate startDate;
    @NotNull
    LocalDate endDate;
    double latitude;
    double longitude;
}
