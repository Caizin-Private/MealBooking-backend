package org.example.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.entity.*;
import org.example.repository.CutoffConfigRepository;
import org.example.repository.MealBookingRepository;
import org.example.repository.NotificationRepository;
import org.example.repository.UserRepository;
import org.example.service.NotificationService;
import org.example.service.PushNotificationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.*;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.task.scheduling", name = "enabled", havingValue = "true")
public class MealMissedBookingScheduler {

    private final UserRepository userRepository;
    private final MealBookingRepository mealBookingRepository;
    private final NotificationRepository notificationRepository;
    private final CutoffConfigRepository cutoffConfigRepository;
    private final PushNotificationService pushNotificationService;
    private final NotificationService notificationService;
    private final Clock clock;

    @Scheduled(cron = "*/3 * * * * *")// 10:30 PM
    public void sendMissedMealBookingNotifications() {

        // ---------- CUTOFF CONFIG ----------
        var cutoffOpt = cutoffConfigRepository.findTopByOrderByIdDesc();
        if (cutoffOpt.isEmpty()) return;

        LocalTime now = LocalTime.now(clock);
        if (now.isBefore(cutoffOpt.get().getCutoffTime())) return;

        LocalDate today = LocalDate.now(clock);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        userRepository.findAll().forEach(user -> {

            // ---------- ONLY USERS ----------
            if (user.getRole() != Role.USER) return;

            // ---------- IF BOOKED, SKIP ----------
            boolean booked =
                    mealBookingRepository.existsByUserAndBookingDate(user, today);
            if (booked) return;

            // ---------- IF ALREADY MARKED MISSED, SKIP ----------
            boolean alreadyMissed =
                    mealBookingRepository.existsByUserAndBookingDateAndStatus(
                            user,
                            today,
                            BookingStatus.MISSED
                    );
            if (alreadyMissed) return;

            // ---------- CREATE MISSED BOOKING ----------
            MealBooking missedBooking = MealBooking.builder()
                    .user(user)
                    .bookingDate(today)
                    .bookedAt(LocalDateTime.now(clock))
                    .status(BookingStatus.MISSED)
                    .build();

            mealBookingRepository.save(missedBooking);

            // ---------- NOTIFICATION IDEMPOTENCY ----------
            boolean alreadyNotified =
                    notificationRepository.existsByUserIdAndTypeAndScheduledAtBetween(
                            user.getId(),
                            NotificationType.MISSED_BOOKING,
                            startOfDay,
                            endOfDay
                    );
            if (alreadyNotified) return;

            // ---------- SAVE NOTIFICATION ----------
            notificationService.schedule(
                    user.getId(),
                    "Meal booking missed",
                    "You missed booking your meal for today.",
                    NotificationType.MISSED_BOOKING,
                    today.atStartOfDay()
            );

            // ---------- PUSH ----------
            pushNotificationService.sendMissedBookingNotification(
                    user.getId(),
                    today
            );
        });
    }
}
