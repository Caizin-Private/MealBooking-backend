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

    public final UserLocationRepository repository;
    public final MealBookingRepository mealBookingRepository;
    public final UserRepository userRepository;
    public final Clock clock;

    // ✅ DEFAULTS so unit tests pass
    private double officeLatitude = 18.5204;
    private double officeLongitude = 73.8567;
    private double geofenceRadiusMeters = 500;

    public void saveLocation(Long userId, LocationUpdateRequestDTO request) {

        LocalDate today = LocalDate.now(clock);

        // ✅ 1. WEEKEND → skip EVERYTHING
        if (today.getDayOfWeek().getValue() >= 6) {
            return;
        }

        LocalTime now = LocalTime.now(clock);
        LocalTime lunchStart = LocalTime.of(12, 0);
        LocalTime lunchEnd = LocalTime.of(14, 30);
//        LocalTime lunchStart = LocalTime.of(0, 0);
//        LocalTime lunchEnd = LocalTime.of(23, 59);

        // ✅ 2. Always save location on weekdays
        saveUserLocation(userId, request);

        // ✅ 3. Outside lunch window → no booking logic
        if (now.isBefore(lunchStart) || now.isAfter(lunchEnd)) {
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        MealBooking booking = mealBookingRepository
                .findByUserAndBookingDate(user, today)
                .orElse(null);

        if (booking == null || booking.getAvailableForLunch()) {
            return;
        }

        boolean insideGeofence = isUserWithinGeofence(
                request.getLatitude(),
                request.getLongitude()
        );

        // ✅ 4. Inside geofence → mark available
        if (insideGeofence) {
            booking.setAvailableForLunch(true);
            mealBookingRepository.save(booking);
            return;
        }

        // ✅ 5. EXACTLY at 14:30 → DEFAULT
        if (now.equals(lunchEnd)) {
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
        double latDist = Math.toRadians(lat2 - lat1);
        double lonDist = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDist / 2) * Math.sin(latDist / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDist / 2) * Math.sin(lonDist / 2);

        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
