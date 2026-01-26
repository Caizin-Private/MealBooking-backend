package org.example.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.entity.NotificationType;
import org.example.entity.Role;
import org.example.repository.MealBookingRepository;
import org.example.repository.NotificationRepository;
import org.example.repository.UserRepository;
import org.example.service.NotificationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.task.scheduling", name = "enabled", havingValue = "true")
public class MealInactivityScheduler {

    private final UserRepository userRepository;
    private final MealBookingRepository mealBookingRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    private static final int INACTIVITY_DAYS = 3;

    @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Kolkata") // 10 AM IST
    public void sendInactivityNudges() {

        LocalDate today = LocalDate.now(clock);
        LocalDate fromDate = today.minusDays(INACTIVITY_DAYS);
        LocalDate toDate = today.minusDays(1);

        LocalDateTime scheduledAt = today.atStartOfDay();

        userRepository.findAll().forEach(user -> {

            if (user.getRole() != Role.USER) return;

            boolean hasRecentBooking =
                    mealBookingRepository.existsByUserAndBookingDateBetween(
                            user, fromDate, toDate
                    );

            if (hasRecentBooking) return;

            boolean alreadyNotified =
                    notificationRepository.existsByUserIdAndTypeAndScheduledAtBetween(
                            user.getId(),
                            NotificationType.INACTIVITY_NUDGE,
                            scheduledAt,
                            today.atTime(23, 59, 59)
                    );

            if (alreadyNotified) return;

            notificationService.schedule(
                    user.getId(),
                    "We miss you!",
                    "You haven’t booked meals in the last few days.",
                    NotificationType.INACTIVITY_NUDGE,
                    scheduledAt
            );
        });
    }
}
