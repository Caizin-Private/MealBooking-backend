package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.BookingStatus;
import org.example.entity.MealBooking;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.MealBookingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MealBookingServiceImpl implements MealBookingService {

    private final MealBookingRepository mealBookingRepository;
    private final GeoFenceService geoFenceService;
    private final PushNotificationService pushNotificationService;

    @Override
    public void bookMeals(
            User user,
            List<LocalDate> bookingDates,
            double latitude,
            double longitude
    ) {
        if (user.getRole() != Role.USER) {
            throw new RuntimeException("Only USER can book meals");
        }

        if (!geoFenceService.isInsideAllowedArea(latitude, longitude)) {
            throw new RuntimeException("User outside allowed location");
        }

        // logic will come here (incrementally)
        MealBooking booking = MealBooking.builder()
                .userId(user.getId())
                .bookingDate(bookingDates.get(0))
                .bookedAt(LocalDateTime.now())
                .status(BookingStatus.BOOKED)
                .build();

        mealBookingRepository.save(booking);
    }
}