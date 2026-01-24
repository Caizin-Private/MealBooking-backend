package org.example.service;

import org.example.dto.CancelMealRequestDTO;
import org.example.dto.MealBookingResponseDTO;
import org.example.dto.RangeMealBookingResponseDTO;
import org.example.dto.UpcomingMealsResponseDTO;
import org.example.entity.User;

import java.time.LocalDate;

public interface MealBookingService {
    MealBookingResponseDTO bookSingleMeal(User user, LocalDate date);

    RangeMealBookingResponseDTO bookRangeMeals(User user, LocalDate startDate, LocalDate endDate);

    UpcomingMealsResponseDTO getUpcomingMeals(User user);

    MealBookingResponseDTO cancelMealByUserIdAndDate(CancelMealRequestDTO request);
}