package org.example.scheduler;


import lombok.RequiredArgsConstructor;
import org.example.entity.CutoffConfig;
import org.example.entity.NotificationType;
import org.example.entity.Role;
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
public class MealReminderScheduler {

    private final UserRepository userRepository;
    private final MealBookingRepository mealBookingRepository;
    private final PushNotificationService pushNotificationService;
    private final CutoffConfigRepository cutoffConfigRepository;
    private final NotificationRepository notificationRepository; // 👈 ADD THIS
    private final Clock clock;

    @Scheduled(cron = "0 0 18 * * *") // 6 PM
    public void sendMealBookingReminders() {

        CutoffConfig cutoffConfig = cutoffConfigRepository
                .findTopByOrderByIdDesc()
                .orElseThrow();

        LocalTime now = LocalTime.now(clock);
        if (now.isAfter(cutoffConfig.getCutoffTime())) {
            return;
        }

        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);

        userRepository.findAll().forEach(user -> {

            if (user.getRole() != Role.USER) {
                return;
            }

            boolean alreadyBooked =
                    mealBookingRepository.existsByUserAndBookingDate(user, tomorrow);

            if (alreadyBooked) {
                return;
            }

            // ---------- IDPOTENCY CHECK ----------
            LocalDateTime startOfDay = tomorrow.atStartOfDay();
            LocalDateTime endOfDay = tomorrow.atTime(23, 59, 59);

            boolean alreadyNotified =
                    notificationRepository.existsByUserIdAndTypeAndScheduledAtBetween(
                            user.getId(),
                            NotificationType.MEAL_REMINDER,
                            startOfDay,
                            endOfDay
                    );

            if (alreadyNotified) {
                return;
            }

            // ---------- CREATE NOTIFICATION ----------
            notificationRepository.save(
                    Notification.builder()
                            .userId(user.getId())
                            .title("Meal booking reminder")
                            .message("Please book your meal for " + tomorrow)
                            .type(NotificationType.MEAL_REMINDER)
                            .scheduledAt(LocalDateTime.now(clock))
                            .sent(true)
                            .sentAt(LocalDateTime.now(clock))
                            .build()
            );

            pushNotificationService.sendMealReminder(user.getId(), tomorrow);
        });
    }

}
