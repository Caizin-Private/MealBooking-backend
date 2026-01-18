package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.BookingStatus;
import org.example.entity.CutoffConfig;
import org.example.entity.MealBooking;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.CutoffConfigRepository;
import org.example.repository.MealBookingRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MealBookingServiceImpl implements MealBookingService {

    private final MealBookingRepository mealBookingRepository;
    private final GeoFenceService geoFenceService;
    private final PushNotificationService pushNotificationService;
    private final CutoffConfigRepository cutoffConfigRepository;
    private final Clock clock;   // ✅ ADD THIS

    @Override
    public void bookMeals(
            User user,
            List<LocalDate> bookingDates,
            double latitude,
            double longitude
    ) {

        if (!geoFenceService.isInsideAllowedArea(latitude, longitude)) {
            throw new RuntimeException("User outside allowed location");
        }

        CutoffConfig cutoffConfig = cutoffConfigRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cutoff config not set"));

        LocalTime cutoffTime = cutoffConfig.getCutoffTime();

        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);

        for (LocalDate date : bookingDates) {

            if (date.equals(today.plusDays(1)) && now.isAfter(cutoffTime)) {
                throw new RuntimeException("Booking closed for tomorrow");
            }

            if (mealBookingRepository.existsByUserIdAndBookingDate(
                    user.getId(), date)) {
                throw new RuntimeException("Meal already booked for this date");
            }

            MealBooking booking = MealBooking.builder()
                    .userId(user.getId())
                    .bookingDate(date)
                    .bookedAt(LocalDateTime.now(clock))
                    .status(BookingStatus.BOOKED)
                    .build();

            mealBookingRepository.save(booking);
        }

        pushNotificationService.sendBookingConfirmation(
                user.getId(),
                bookingDates
        );
    }
}
