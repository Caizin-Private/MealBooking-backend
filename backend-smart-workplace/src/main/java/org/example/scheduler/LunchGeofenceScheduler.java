package org.example.scheduler;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.task.scheduling", name = "enabled", havingValue = "true")
public class LunchGeofenceScheduler {

    private final MealBookingRepository mealBookingRepository;
    private final UserLocationRepository userLocationRepository;
    private final OfficeLocationConfig officeLocationConfig;
    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;
    private final NotificationRepository notificationRepository;
    private final Clock clock;

    @Scheduled(cron = "*/10 * * * * *")
    @Scheduled(fixedRate = 30000) // 30 sec for testing
    @Transactional
    public void autoDefaultLunchBookings() {

        LocalDate today = LocalDate.now(clock);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        List<MealBooking> bookings =
                mealBookingRepository.findByBookingDateAndStatus(
                        today,
                        BookingStatus.BOOKED
                );

        for (MealBooking booking : bookings) {

            Long userId = booking.getUser().getId();

            userLocationRepository.findById(userId).ifPresent(location -> {

                double distance = GeoUtils.distanceInMeters(
                        location.getLatitude(),
                        location.getLongitude(),
                        officeLocationConfig.getLatitude(),
                        officeLocationConfig.getLongitude()
                );

                if (distance > officeLocationConfig.getRadiusMeters()) {

                    // 1️⃣ Update booking
                    booking.setStatus(BookingStatus.DEFAULT);

                    // 2️⃣ Idempotency check
                    boolean alreadyNotified =
                            notificationRepository
                                    .existsByUserIdAndTypeAndScheduledAtBetween(
                                            userId,
                                            NotificationType.LUNCH_DEFAULTED,
                                            startOfDay,
                                            endOfDay
                                    );

                    if (alreadyNotified) {
                        return;
                    }

                    // 3️⃣ Save notification
                    notificationService.schedule(
                            userId,
                            "Lunch auto-cancelled",
                            "You were not near the office during lunch time.",
                            NotificationType.LUNCH_DEFAULTED,
                            LocalDateTime.now(clock)
                    );

                    // 4️⃣ Push (stub / real later)
                    pushNotificationService
                            .sendLunchDefaultedNotification(userId, today);
                }
            });
        }
    }
}
