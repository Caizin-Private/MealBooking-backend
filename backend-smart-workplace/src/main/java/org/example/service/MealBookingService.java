package org.example.service;

import org.example.entity.User;

import java.time.LocalDate;
import java.util.List;

public interface MealBookingService {

    void bookMeals(
            User user,
            List<LocalDate> bookingDates,
            double latitude,
            double longitude
    );
}