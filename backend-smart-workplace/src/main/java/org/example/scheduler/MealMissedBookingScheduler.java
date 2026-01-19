package org.example.scheduler;
import lombok.RequiredArgsConstructor;
import org.example.entity.*;
import org.example.repository.CutoffConfigRepository;
import org.example.repository.MealBookingRepository;
import org.example.repository.NotificationRepository;
import org.example.repository.UserRepository;
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
    private final Clock clock;

    @Scheduled(cron = "0 30 22 * * *") // 10:30 PM (after cutoff)
    public void sendMissedMealBookingNotifications() {

        CutoffConfig cutoffConfig = cutoffConfigRepository
                .findTopByOrderByIdDesc()
                .orElseThrow();

        LocalTime now = LocalTime.now(clock);

        // Only AFTER cutoff
        if (now.isBefore(cutoffConfig.getCutoffTime())) {
            return;
        }

        LocalDate today = LocalDate.now(clock);

        userRepository.findAll().forEach(user -> {

            if (user.getRole() != Role.USER) {
                return;
            }

            boolean booked =
                    mealBookingRepository.existsByUserAndBookingDate(user, today);

            if (booked) {
                return;
            }

            // ---------- IDPOTENCY CHECK ----------
            LocalDateTime start = today.atStartOfDay();
            LocalDateTime end = today.atTime(23, 59, 59);

            boolean alreadyNotified =
                    notificationRepository.existsByUserIdAndTypeAndScheduledAtBetween(
                            user.getId(),
                            NotificationType.MISSED_BOOKING,
                            start,
                            end
                    );

            if (alreadyNotified) {
                return;
            }

            // ---------- SAVE NOTIFICATION ----------
            notificationRepository.save(
                    Notification.builder()
                            .userId(user.getId())
                            .title("Meal booking missed")
                            .message("You missed booking your meal for today.")
                            .type(NotificationType.MISSED_BOOKING)
                            .scheduledAt(LocalDateTime.now(clock))
                            .sent(true)
                            .sentAt(LocalDateTime.now(clock))
                            .build()
            );

            pushNotificationService.sendMissedBookingNotification(
                    user.getId(),
                    today
            );
        });
    }
}
