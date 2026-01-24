package org.example.scheduler;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.config.OfficeLocationConfig;
import org.example.entity.BookingStatus;
import org.example.entity.MealBooking;
import org.example.entity.NotificationType;
import org.example.repository.MealBookingRepository;
import org.example.repository.NotificationRepository;
import org.example.repository.UserLocationRepository;
import org.example.service.NotificationService;
import org.example.service.PushNotificationService;
import org.example.utils.GeoUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.task.scheduling", name = "enabled", havingValue = "true")
@Slf4j
public class LunchGeofenceScheduler {

    private final MealBookingRepository mealBookingRepository;
    private final UserLocationRepository userLocationRepository;
    private final OfficeLocationConfig officeLocationConfig;
    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;
    private final NotificationRepository notificationRepository;
    private final Clock clock;

    // Lunch time configuration (12:00 to 15:00)
    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(15, 0);

    // Half-hourly check schedule: 12:00, 12:30, 13:00, 13:30, 14:00, 14:30
    @Scheduled(cron = "0 0,30 12-14 * * *") // At minute 0 and 30, between 12:00 and 14:30
    @Transactional
    public void checkUserLocationForLunchAvailability() {

        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);

        log.info("Running lunch geofence check at {} on {}", now, today);

        // Check if current time is within lunch period (12:00 - 14:30)
        if (now.isBefore(LUNCH_START) || now.isAfter(LocalTime.of(14, 30))) {
            log.debug("Current time {} is outside lunch geofence window", now);
            return;
        }

        // Get all booked meals for today where availableForLunch is still false
        List<MealBooking> bookingsToCheck = mealBookingRepository
                .findByBookingDateAndAvailableForLunch(today, false);

        log.info("Found {} bookings to check for lunch availability", bookingsToCheck.size());

        for (MealBooking booking : bookingsToCheck) {
            checkAndUpdateUserLocation(booking, today, now);
        }
    }

    private void checkAndUpdateUserLocation(MealBooking booking, LocalDate today, LocalTime now) {

        Long userId = booking.getUser().getId();

        userLocationRepository.findById(userId).ifPresentOrElse(location -> {

            // Calculate distance from office
            double distance = GeoUtils.distanceInMeters(
                    location.getLatitude(),
                    location.getLongitude(),
                    officeLocationConfig.getLatitude(),
                    officeLocationConfig.getLongitude()
            );

            log.debug("User {} is {} meters from office", userId, distance);

            // Check if user is within 500m radius
            if (distance <= officeLocationConfig.getRadiusMeters()) {

                // User is within office radius - mark as available for lunch
                booking.setAvailableForLunch(true);
                mealBookingRepository.save(booking);

                log.info("User {} marked as available for lunch (distance: {}m)", userId, distance);

                // Send notification that user is available for lunch
                sendLunchAvailabilityNotification(userId, today);

            } else {

                log.debug("User {} is outside office radius ({}m)", userId, distance);

                // Check if this is the last check (14:30) and user is still unavailable
                if (now.equals(LocalTime.of(14, 30))) {

                    // Mark booking as DEFAULT since user was never available during lunch hours
                    booking.setStatus(BookingStatus.DEFAULT);
                    mealBookingRepository.save(booking);

                    log.warn("User {} never available during lunch hours - booking marked as DEFAULT", userId);

                    // Send notification that lunch was defaulted
                    sendLunchDefaultedNotification(userId, today, distance);
                }
            }

        }, () -> {
            log.warn("No location data found for user {}", userId);

            // If no location data and this is the last check, mark as DEFAULT
            if (now.equals(LocalTime.of(14, 30))) {
                booking.setStatus(BookingStatus.DEFAULT);
                mealBookingRepository.save(booking);

                log.warn("No location data for user {} at last check - booking marked as DEFAULT", userId);
                sendLunchDefaultedNotification(userId, today, -1);
            }
        });
    }

    private void sendLunchAvailabilityNotification(Long userId, LocalDate today) {

        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        // Check if notification already sent today
        boolean alreadyNotified = notificationRepository
                .existsByUserIdAndTypeAndScheduledAtBetween(
                        userId,
                        NotificationType.LUNCH_AVAILABLE,
                        startOfDay,
                        endOfDay
                );

        if (!alreadyNotified) {
            notificationService.schedule(
                    userId,
                    "Available for lunch",
                    "You are within office radius and marked as available for lunch.",
                    NotificationType.LUNCH_AVAILABLE,
                    LocalDateTime.now(clock)
            );

            pushNotificationService.sendLunchAvailableNotification(userId, today);
        }
    }

    private void sendLunchDefaultedNotification(Long userId, LocalDate today, double distance) {

        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        // Check if notification already sent today
        boolean alreadyNotified = notificationRepository
                .existsByUserIdAndTypeAndScheduledAtBetween(
                        userId,
                        NotificationType.LUNCH_DEFAULTED,
                        startOfDay,
                        endOfDay
                );

        if (!alreadyNotified) {
            String message = distance > 0
                    ? String.format("You were never within 500m of office during lunch hours (last checked: %.0fm away)", distance)
                    : "No location data available during lunch hours";

            notificationService.schedule(
                    userId,
                    "Lunch auto-cancelled",
                    message,
                    NotificationType.LUNCH_DEFAULTED,
                    LocalDateTime.now(clock)
            );

            pushNotificationService.sendLunchDefaultedNotification(userId, today);
        }
    }
}
