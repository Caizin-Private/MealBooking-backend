package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.LocationUpdateRequestDTO;
import org.example.entity.BookingStatus;
import org.example.entity.MealBooking;
import org.example.entity.User;
import org.example.entity.UserLocation;
import org.example.repository.MealBookingRepository;
import org.example.repository.UserLocationRepository;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class UserLocationService {

    private final UserLocationRepository repository;
    private final MealBookingRepository mealBookingRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    // Office configuration from application.yml
    @Value("${office.latitude}")
    private double officeLatitude;

    @Value("${office.longitude}")
    private double officeLongitude;

    @Value("${office.radius-meters}")
    private double geofenceRadiusMeters;

    public void saveLocation(Long userId, LocationUpdateRequestDTO request) {
        LocalDate today = LocalDate.now(clock);
        if (today.getDayOfWeek().getValue() >= 6) {
            return;
        }

        LocalTime now = LocalTime.now(clock);
        LocalTime lunchStart = LocalTime.of(12, 0);
        LocalTime lunchEnd = LocalTime.of(14, 30);

        if (now.isBefore(lunchStart) || now.isAfter(lunchEnd)) {
            saveUserLocation(userId, request);
            return;
        }

        saveUserLocation(userId, request);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        var todayBooking = mealBookingRepository.findByUserAndBookingDate(user, today);

        if (todayBooking.isEmpty() || todayBooking.get().getAvailableForLunch()) {
            return;
        }

        boolean isWithinGeofence = isUserWithinGeofence(
                request.getLatitude(),
                request.getLongitude()
        );

        if (isWithinGeofence) {
            MealBooking booking = todayBooking.get();
            booking.setAvailableForLunch(true);
            mealBookingRepository.save(booking);
        } else if (now.equals(lunchEnd) || now.isAfter(lunchEnd.minusMinutes(30))) {
            MealBooking booking = todayBooking.get();
            booking.setStatus(BookingStatus.DEFAULT);
            mealBookingRepository.save(booking);
        }
    }

    private void saveUserLocation(Long userId, LocationUpdateRequestDTO request) {
        UserLocation location = UserLocation.builder()
                .userId(userId)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .updatedAt(LocalDateTime.now(clock))
                .build();

        repository.save(location);
    }

    private boolean isUserWithinGeofence(double userLat, double userLon) {
        double distance = calculateDistance(
                userLat, userLon,
                officeLatitude, officeLongitude
        );

        return distance <= geofenceRadiusMeters;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}
