package org.example.scheduler;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.config.OfficeLocationConfig;
import org.example.entity.BookingStatus;
import org.example.entity.MealBooking;
import org.example.entity.NotificationType;
import org.example.repository.MealBookingRepository;
import org.example.repository.UserLocationRepository;
import org.example.service.NotificationService;
import org.example.service.PushNotificationService;
import org.example.utils.GeoUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LunchGeofenceScheduler {

    private final MealBookingRepository mealBookingRepository;
    private final UserLocationRepository userLocationRepository;
    private final OfficeLocationConfig officeLocationConfig;
    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;
    private final Clock clock;

    // Runs every day at 1:00 PM
    @Scheduled(cron = "*/30 * * * * *")
    @Transactional
    public void autoDefaultLunchBookings() {

        LocalDate today = LocalDate.now(clock);

        List<MealBooking> bookings =
                mealBookingRepository.findByBookingDateAndStatus(
                        today,
                        BookingStatus.BOOKED
                );

        for (MealBooking booking : bookings) {

            Long userId = booking.getUser().getId();

            userLocationRepository.findById(userId)
                    .ifPresent(location -> {

                        double distance =
                                GeoUtils.distanceInMeters(
                                        location.getLatitude(),
                                        location.getLongitude(),
                                        officeLocationConfig.getLatitude(),
                                        officeLocationConfig.getLongitude()
                                );

                        if (distance > officeLocationConfig.getRadiusMeters()) {

                            booking.setStatus(BookingStatus.DEFAULT);

                            notificationService.schedule(
                                    userId,
                                    "Lunch auto-cancelled",
                                    "You were not near the office during lunch time.",
                                    NotificationType.MISSED_BOOKING,
                                    LocalDateTime.now(clock)
                            );

                            pushNotificationService
                                    .sendMissedBookingNotification(userId, today);
                        }
                    });
        }
    }
}
