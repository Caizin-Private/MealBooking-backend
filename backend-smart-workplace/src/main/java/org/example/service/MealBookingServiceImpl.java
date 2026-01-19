package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.BookingStatus;
import org.example.entity.CutoffConfig;
import org.example.entity.MealBooking;
import org.example.entity.User;
import org.example.repository.CutoffConfigRepository;
import org.example.repository.MealBookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Transactional
public class MealBookingServiceImpl implements MealBookingService {

    private final MealBookingRepository mealBookingRepository;
    private final GeoFenceService geoFenceService;
    private final PushNotificationService pushNotificationService;
    private final CutoffConfigRepository cutoffConfigRepository;
    private final Clock clock;

    @Override
    public void bookMeals(
            User user,
            LocalDate startDate,
            LocalDate endDate,
            double latitude,
            double longitude
    ) {

        /* 1️⃣ Geo-fence validation */
        if (!geoFenceService.isInsideAllowedArea(latitude, longitude)) {
            throw new RuntimeException("User outside allowed location");
        }

        /* 2️⃣ Date range validation */
        LocalDate today = LocalDate.now(clock);

        if (startDate.isBefore(today)) {
            throw new RuntimeException("Cannot book meals for past dates");
        }

        if (endDate.isBefore(startDate)) {
            throw new RuntimeException("End date cannot be before start date");
        }

        /* 3️⃣ Cutoff config */
        CutoffConfig cutoffConfig = cutoffConfigRepository
                .findTopByOrderByIdDesc()
                .orElseThrow(() -> new RuntimeException("Cutoff config not set"));

        LocalTime now = LocalTime.now(clock);

        /* 4️⃣ Loop through date range */
        for (LocalDate date = startDate;
             !date.isAfter(endDate);
             date = date.plusDays(1)) {

            validateBookingDate(
                    date,
                    today,
                    now,
                    cutoffConfig,
                    user
            );

            MealBooking booking = MealBooking.builder()
                    .user(user)
                    .bookingDate(date)
                    .bookedAt(LocalDateTime.now(clock))
                    .status(BookingStatus.BOOKED)
                    .build();

            mealBookingRepository.save(booking);
        }

        /* 5️⃣ Push notification (once for whole range) */
        pushNotificationService.sendBookingConfirmation(
                user.getId(),
                startDate,
                endDate
        );
    }

    /* 🔒 Internal per-date business rules */
    private void validateBookingDate(
            LocalDate date,
            LocalDate today,
            LocalTime now,
            CutoffConfig cutoffConfig,
            User user
    ) {

        /* Cutoff applies only for tomorrow */
        if (date.equals(today.plusDays(1))
                && now.isAfter(cutoffConfig.getCutoffTime())) {
            throw new RuntimeException("Booking closed for " + date);
        }

        /* Duplicate booking check */
        if (mealBookingRepository.existsByUserAndBookingDate(user, date)) {
            throw new RuntimeException("Meal already booked for " + date);
        }
    }
}
