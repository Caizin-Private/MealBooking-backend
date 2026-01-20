package org.example.scheduler;


import lombok.RequiredArgsConstructor;
import org.example.entity.CutoffConfig;
import org.example.entity.NotificationType;
import org.example.entity.Role;
import org.example.repository.CutoffConfigRepository;
import org.example.repository.MealBookingRepository;
import org.example.repository.NotificationRepository;
import org.example.repository.UserRepository;
import org.example.service.NotificationService;
import org.example.service.PushNotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.example.entity.Notification;

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
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    @Scheduled(cron = "0 0 18 * * *") // 6 PM
    public void sendMealBookingReminders() {

        Optional<CutoffConfig> cutoffOpt =
                cutoffConfigRepository.findTopByOrderByIdDesc();

        if (cutoffOpt.isEmpty()) {
            return;
        }

        CutoffConfig cutoffConfig = cutoffOpt.get();

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

            // idempotency check
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

            notificationService.schedule(
                    user.getId(),
                    "Meal booking reminder",
                    "Please book your meal for " + tomorrow,
                    NotificationType.MEAL_REMINDER,
                    LocalDateTime.now(clock)
            );


            pushNotificationService.sendMealReminder(user.getId(), tomorrow);
        });
    }


}
