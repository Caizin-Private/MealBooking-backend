package org.example.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.entity.Notification;
import org.example.entity.NotificationType;
import org.example.entity.Role;
import org.example.repository.MealBookingRepository;
import org.example.repository.NotificationRepository;
import org.example.repository.UserRepository;
import org.example.service.PushNotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class MealInactivityScheduler {

    private final UserRepository userRepository;
    private final MealBookingRepository mealBookingRepository;
    private final NotificationRepository notificationRepository;
    private final PushNotificationService pushNotificationService;
    private final Clock clock;

    private static final int INACTIVITY_DAYS = 3;

    @Scheduled(cron = "0 0 10 * * *") // 10 AM daily
    public void sendInactivityNudges() {

        LocalDate today = LocalDate.now(clock);

        // ✔ Correct inactivity window
        LocalDate fromDate = today.minusDays(INACTIVITY_DAYS);
        LocalDate toDate = today.minusDays(1);

        userRepository.findAll().forEach(user -> {

            if (user.getRole() != Role.USER) {
                return;
            }

            boolean hasRecentBooking =
                    mealBookingRepository.existsByUserAndBookingDateBetween(
                            user,
                            fromDate,
                            toDate
                    );

            // ✔ IMPORTANT early return
            if (hasRecentBooking) {
                return;
            }

            // ---------- IDEMPOTENCY CHECK ----------
            LocalDateTime start = today.atStartOfDay();
            LocalDateTime end = today.atTime(23, 59, 59);

            boolean alreadyNotified =
                    notificationRepository.existsByUserIdAndTypeAndScheduledAtBetween(
                            user.getId(),
                            NotificationType.INACTIVITY_NUDGE,
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
                            .title("We miss you!")
                            .message("You haven’t booked meals in the last few days.")
                            .type(NotificationType.INACTIVITY_NUDGE)
                            .scheduledAt(LocalDateTime.now(clock))
                            .sent(false)
                            .sentAt(null)
                            .build()
            );

            pushNotificationService.sendInactivityNudge(user.getId());
        });
    }
}
