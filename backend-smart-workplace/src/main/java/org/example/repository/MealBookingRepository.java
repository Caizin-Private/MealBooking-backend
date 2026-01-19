package org.example.repository;

import org.example.entity.MealBooking;
import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface MealBookingRepository extends JpaRepository<MealBooking, Long> {

    boolean existsByUserAndBookingDate(User user, LocalDate bookingDate);
}
