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
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.task.scheduling", name = "enabled", havingValue = "true")
public class MealReminderScheduler {

    private final UserRepository userRepository;
    private final MealBookingRepository mealBookingRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    @Scheduled(cron = "0 0 18 * * *", zone = "Asia/Kolkata") // 6 PM IST
    public void sendMealBookingReminders() {

        LocalTime cutoffTime = LocalTime.of(22, 0);
        if (LocalTime.now(clock).isAfter(cutoffTime)) return;

        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        if (tomorrow.getDayOfWeek().getValue() >= 6) return;

        LocalDateTime scheduledAt = LocalDateTime.now(clock); // Send NOW (Monday 6 PM)

        userRepository.findAll().forEach(user -> {

            if (user.getRole() != Role.USER) return;

            boolean alreadyBooked =
                    mealBookingRepository.existsByUserAndBookingDate(user, tomorrow);
            if (alreadyBooked) return;

            // Check if reminder for tomorrow's meal was already sent today
            boolean alreadyRemindedToday =
                    notificationRepository.existsByUserIdAndTypeAndScheduledAtBetween(
                            user.getId(),
                            NotificationType.MEAL_REMINDER,
                            LocalDate.now(clock).atStartOfDay(),     // Today 12:00 AM
                            LocalDate.now(clock).atTime(23, 59, 59)  // Today 11:59 PM
                    );
            if (alreadyRemindedToday) return;

            notificationService.createAndSendImmediately(
                    user.getId(),
                    "Meal booking reminder",
                    "Please book your meal for " + tomorrow + " before 10 PM",
                    NotificationType.MEAL_REMINDER
            );
        });
    }
}
