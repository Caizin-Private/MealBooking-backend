package org.example.repository;

import org.example.entity.MealBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface MealBookingRepository
        extends JpaRepository<MealBooking, Long> {

    boolean existsByUserIdAndBookingDate(Long userId, LocalDate bookingDate);
}