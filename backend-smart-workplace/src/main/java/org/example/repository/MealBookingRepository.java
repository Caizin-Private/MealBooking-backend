package org.example.repository;

import org.example.entity.MealBooking;
import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MealBookingRepository extends JpaRepository<MealBooking, Long> {

    boolean existsByUserAndBookingDate(User user, LocalDate bookingDate);
    Optional<MealBooking> findByUserAndBookingDate(User user, LocalDate date);

    boolean existsByUserAndBookingDateBetween(
            User user,
            LocalDate start,
            LocalDate end
    );

    boolean existsByUserAndBookingDateAfter(User user, LocalDate date);

}
