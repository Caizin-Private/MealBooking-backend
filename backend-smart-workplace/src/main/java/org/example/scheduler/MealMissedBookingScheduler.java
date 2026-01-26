package org.example.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.entity.*;
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
public class MealMissedBookingScheduler {

    private final UserRepository userRepository;
    private final MealBookingRepository mealBookingRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    @Scheduled(cron = "0 30 22 * * *", zone = "Asia/Kolkata") // 10:30 PM IST
    public void sendMissedMealBookingNotifications() {

        LocalDate today = LocalDate.now(clock);

        if (today.getDayOfWeek().getValue() >= 6) return;

        LocalDateTime scheduledAt = today.atStartOfDay();

        userRepository.findAll().forEach(user -> {

            if (user.getRole() != Role.USER) return;

            boolean booked =
                    mealBookingRepository.existsByUserAndBookingDate(user, today);
            if (booked) return;

            boolean alreadyMissed =
                    mealBookingRepository.existsByUserAndBookingDateAndStatus(
                            user, today, BookingStatus.MISSED
                    );
            if (alreadyMissed) return;

            mealBookingRepository.save(
                    MealBooking.builder()
                            .user(user)
                            .bookingDate(today)
                            .bookedAt(LocalDateTime.now(clock))
                            .status(BookingStatus.MISSED)
                            .build()
            );

            boolean alreadyNotified =
                    notificationRepository.existsByUserIdAndTypeAndScheduledAtBetween(
                            user.getId(),
                            NotificationType.MISSED_BOOKING,
                            scheduledAt,
                            today.atTime(23, 59, 59)
                    );
            if (alreadyNotified) return;

            notificationService.schedule(
                    user.getId(),
                    "Meal booking missed",
                    "You missed booking your meal for today.",
                    NotificationType.MISSED_BOOKING,
                    scheduledAt
            );
        });
    }
}
