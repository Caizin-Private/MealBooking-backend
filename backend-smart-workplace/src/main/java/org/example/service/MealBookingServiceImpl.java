package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.MealBookingResponseDTO;
import org.example.dto.RangeMealBookingResponseDTO;
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
import java.util.ArrayList;
import java.util.List;

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

    public void cancelMeal(User user, LocalDate date) {

        LocalDate today = LocalDate.now(clock);

        if (date.isBefore(today)) {
            throw new RuntimeException("Past bookings cannot be cancelled");
        }

        MealBooking booking =
                mealBookingRepository
                        .findByUserAndBookingDate(user, date)
                        .orElseThrow(() -> new RuntimeException("No booking found for this date"));

        mealBookingRepository.delete(booking);

        pushNotificationService.sendCancellationConfirmation(
                user.getId(),
                date
        );
    }


    @Override
    public MealBookingResponseDTO bookSingleMeal(User user, LocalDate date) {
        try {
            LocalDate today = LocalDate.now(clock);
            if (date.isBefore(today)) {
                return MealBookingResponseDTO.failure("Cannot book meals for past dates");
            }

            if (date.getDayOfWeek().getValue() >= 6) {
                return MealBookingResponseDTO.failure("Cannot book meals on weekends (Saturday and Sunday)");
            }

            LocalTime now = LocalTime.now(clock);
            if (date.equals(today.plusDays(1)) && now.isAfter(LocalTime.of(22, 0))) {
                return MealBookingResponseDTO.failure("Booking closed for tomorrow after 10 PM");
            }

            if (mealBookingRepository.existsByUserAndBookingDate(user, date)) {
                return MealBookingResponseDTO.failure("Meal already booked for " + date);
            }

            MealBooking booking = MealBooking.builder()
                    .user(user)
                    .bookingDate(date)
                    .bookedAt(LocalDateTime.now(clock))
                    .status(BookingStatus.BOOKED)
                    .availableForLunch(true)
                    .build();

            MealBooking savedBooking = mealBookingRepository.save(booking);

            pushNotificationService.sendSingleMealBookingConfirmation(user.getId(), date);

            return MealBookingResponseDTO.success(
                    "Meal booked successfully for " + date,
                    savedBooking.getId(),
                    date.toString()
            );

        } catch (Exception e) {
            return MealBookingResponseDTO.failure("Booking failed: " + e.getMessage());
        }
    }


    @Override
    public RangeMealBookingResponseDTO bookRangeMeals(User user, LocalDate startDate, LocalDate endDate) {
        List<String> bookedDates = new ArrayList<>();
        List<String> failedBookings = new ArrayList<>();

        try {
            LocalDate today = LocalDate.now(clock);
            LocalTime now = LocalTime.now(clock);

            if (startDate.isBefore(today)) {
                return RangeMealBookingResponseDTO.failure("Cannot book meals for past dates", null);
            }

            if (endDate.isBefore(startDate)) {
                return RangeMealBookingResponseDTO.failure("End date cannot be before start date", null);
            }

            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                try {

                    if (date.getDayOfWeek().getValue() >= 6) {
                        failedBookings.add(date + " - Cannot book on weekends (Saturday and Sunday)");
                        continue;
                    }

                    if (date.equals(today.plusDays(1)) && now.isAfter(LocalTime.of(22, 0))) {
                        failedBookings.add(date + " - Booking closed after 10 PM");
                        continue;
                    }

                    if (mealBookingRepository.existsByUserAndBookingDate(user, date)) {
                        failedBookings.add(date + " - Already booked");
                        continue;
                    }

                    MealBooking booking = MealBooking.builder()
                            .user(user)
                            .bookingDate(date)
                            .bookedAt(LocalDateTime.now(clock))
                            .status(BookingStatus.BOOKED)
                            .availableForLunch(true)
                            .build();

                    mealBookingRepository.save(booking);
                    bookedDates.add(date.toString());

                } catch (Exception e) {
                    failedBookings.add(date + " - " + e.getMessage());
                }
            }

            if (!bookedDates.isEmpty()) {
                pushNotificationService.sendBookingConfirmation(user.getId(), startDate, endDate);
            }

            if (bookedDates.isEmpty()) {
                return RangeMealBookingResponseDTO.failure("No meals were booked", failedBookings);
            } else if (failedBookings.isEmpty()) {
                return RangeMealBookingResponseDTO.success(
                        "All meals booked successfully from " + startDate + " to " + endDate,
                        bookedDates
                );
            } else {
                return RangeMealBookingResponseDTO.success(
                        "Some meals booked successfully from " + startDate + " to " + endDate,
                        bookedDates
                );
            }

        } catch (Exception e) {
            return RangeMealBookingResponseDTO.failure("Range booking failed: " + e.getMessage(), failedBookings);
        }



    }
}
