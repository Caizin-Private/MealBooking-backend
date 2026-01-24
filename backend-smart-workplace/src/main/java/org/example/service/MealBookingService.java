package org.example.service;

import org.example.dto.MealBookingResponseDTO;
import org.example.dto.RangeMealBookingResponseDTO;
import org.example.entity.User;

import java.time.LocalDate;

public interface MealBookingService {
    MealBookingResponseDTO bookSingleMeal(User user, LocalDate date);

    RangeMealBookingResponseDTO bookRangeMeals(User user, LocalDate startDate, LocalDate endDate);

    void bookMeals(
            User user,
            LocalDate startDate,
            LocalDate endDate,
            double latitude,
            double longitude
    );

    void cancelMeal(User user, LocalDate date);
}
