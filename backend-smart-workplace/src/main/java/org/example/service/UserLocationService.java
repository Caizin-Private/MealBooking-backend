package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.config.OfficeLocationConfig;
import org.example.dto.LocationUpdateRequestDTO;
import org.example.entity.BookingStatus;
import org.example.entity.MealBooking;
import org.example.entity.User;
import org.example.entity.UserLocation;
import org.example.repository.MealBookingRepository;
import org.example.repository.UserLocationRepository;
import org.example.repository.UserRepository;
import org.example.utils.GeoUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserLocationService {

    private final UserLocationRepository repository;
    private final MealBookingRepository mealBookingRepository;
    private final UserRepository userRepository;
    private final OfficeLocationConfig officeLocationConfig;
    private final PushNotificationService pushNotificationService;
    private final Clock clock;

    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(15, 0);
    private static final LocalTime FINAL_CHECK_TIME = LocalTime.of(14, 30);
    private static final double LUNCH_RADIUS_METERS = 500.0;

    public void saveLocation(Long userId, LocationUpdateRequestDTO request) {

        UserLocation location = UserLocation.builder()
                .userId(userId)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .updatedAt(LocalDateTime.now(clock))
                .build();

        repository.save(location);

        evaluateLunchAvailability(userId, request.getLatitude(), request.getLongitude());
    }

    private void evaluateLunchAvailability(Long userId, double latitude, double longitude) {

        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);

        if (!isWithinLunchWindow(now)) {
            return;
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return;
        }

        Optional<MealBooking> bookingOpt =
                mealBookingRepository.findByUserAndBookingDate(userOpt.get(), today);

        if (bookingOpt.isEmpty()) {
            return;
        }

        MealBooking booking = bookingOpt.get();


        if (booking.getStatus() != BookingStatus.BOOKED) {
            return;
        }

        if (Boolean.TRUE.equals(booking.getAvailableForLunch())) {
            return;
        }


        double distance = GeoUtils.distanceInMeters(
                latitude,
                longitude,
                officeLocationConfig.getLatitude(),
                officeLocationConfig.getLongitude()
        );


        if (distance <= LUNCH_RADIUS_METERS) {
            booking.setAvailableForLunch(true);
            mealBookingRepository.save(booking);
            return;
        }

        if (isFinalCheckTime(now)) {
            booking.setStatus(BookingStatus.DEFAULT);
            booking.setAvailableForLunch(false);
            mealBookingRepository.save(booking);
            pushNotificationService.sendLunchDefaultedNotification(userId, today);
        }
    }

    private boolean isWithinLunchWindow(LocalTime time) {
        return !time.isBefore(LUNCH_START) && !time.isAfter(LUNCH_END);
    }

    private boolean isFinalCheckTime(LocalTime time) {
        return time.getHour() == FINAL_CHECK_TIME.getHour()
                && time.getMinute() == FINAL_CHECK_TIME.getMinute();
    }
}
