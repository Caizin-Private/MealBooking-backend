package org.example.repository;

import org.example.entity.BookingStatus;
import org.example.entity.MealBooking;
import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
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

    List<MealBooking> findByBookingDateAndStatus(
            LocalDate bookingDate,
            BookingStatus status
    );

    boolean existsByUserAndBookingDateAndStatus(
            User user,
            LocalDate bookingDate,
            BookingStatus status
    );

    List<MealBooking> findByUserOrderByBookingDateDesc(User user);

    List<MealBooking> findByUserAndStatusOrderByBookingDateDesc(User user, BookingStatus status);

    List<MealBooking> findByBookingDateAndAvailableForLunch(
            LocalDate bookingDate,
            Boolean availableForLunch
    );
}



