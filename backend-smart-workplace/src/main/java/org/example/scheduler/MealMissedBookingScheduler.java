package org.example.scheduler;
import lombok.RequiredArgsConstructor;
import org.example.entity.*;
import org.example.repository.CutoffConfigRepository;
import org.example.repository.MealBookingRepository;
import org.example.repository.NotificationRepository;
import org.example.repository.UserRepository;
import org.example.service.NotificationService;
import org.example.service.PushNotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MealMissedBookingScheduler {

    private final UserRepository userRepository;
    private final MealBookingRepository mealBookingRepository;
    private final NotificationRepository notificationRepository;
    private final CutoffConfigRepository cutoffConfigRepository;
    private final PushNotificationService pushNotificationService;
    private final NotificationService notificationService;
    private final Clock clock;

    @Scheduled(cron = "0 30 22 * * *") // 10:30 PM
    public void sendMissedMealBookingNotifications() {

        Optional<CutoffConfig> cutoffOpt =
                cutoffConfigRepository.findTopByOrderByIdDesc();

        if (cutoffOpt.isEmpty()) {
            return; // ✅ NEVER throw from schedulers
        }

        CutoffConfig cutoffConfig = cutoffOpt.get();
        LocalTime now = LocalTime.now(clock);

        // Only AFTER cutoff
        if (now.isBefore(cutoffConfig.getCutoffTime())) {
            return;
        }

        LocalDate today = LocalDate.now(clock);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        userRepository.findAll().forEach(user -> {

            if (user.getRole() != Role.USER) {
                return;
            }

            boolean booked =
                    mealBookingRepository.existsByUserAndBookingDate(user, today);

            if (booked) {
                return;
            }

            // ---------- IDEMPOTENCY CHECK ----------
            boolean alreadyNotified =
                    notificationRepository.existsByUserIdAndTypeAndScheduledAtBetween(
                            user.getId(),
                            NotificationType.MISSED_BOOKING,
                            startOfDay,
                            endOfDay
                    );

            if (alreadyNotified) {
                return;
            }

            // ---------- SAVE NOTIFICATION ----------
            notificationService.schedule(
                    user.getId(),
                    "Meal booking missed",
                    "You missed booking your meal for today.",
                    NotificationType.MISSED_BOOKING,
                    today.atStartOfDay()   // business date (important)
            );

            // ---------- SEND ----------
            pushNotificationService.sendMissedBookingNotification(
                    user.getId(),
                    today
            );
        });
    }
}

