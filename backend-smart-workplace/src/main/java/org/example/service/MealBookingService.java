package org.example.service;

import org.example.entity.User;

import java.time.LocalDate;

public interface MealBookingService {

    void bookMeals(
            User user,
            LocalDate startDate,
            LocalDate endDate,
            double latitude,
            double longitude
    );

    void cancelMeal(User user, LocalDate date);
}
