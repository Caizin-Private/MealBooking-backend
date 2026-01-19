package org.example.scheduler;


import lombok.RequiredArgsConstructor;
import org.example.entity.CutoffConfig;
import org.example.entity.Role;
import org.example.repository.CutoffConfigRepository;
import org.example.repository.MealBookingRepository;
import org.example.repository.UserRepository;
import org.example.service.PushNotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MealReminderScheduler {

    private final UserRepository userRepository;
    private final MealBookingRepository mealBookingRepository;
    private final PushNotificationService pushNotificationService;
    private final CutoffConfigRepository cutoffConfigRepository;
    private final Clock clock;

    // 🔔 Runs every day at 6 PM
    @Scheduled(cron = "0 0 18 * * *")
    public void sendMealBookingReminders() {

        cutoffConfigRepository.findTopByOrderByIdDesc()
                .ifPresent(cutoffConfig -> {

                    LocalTime now = LocalTime.now(clock);

                    if (now.isAfter(cutoffConfig.getCutoffTime())) {
                        return;
                    }

                    LocalDate tomorrow = LocalDate.now(clock).plusDays(1);

                    userRepository.findAll().forEach(user -> {

                        if (user.getRole() != Role.USER) return;

                        boolean alreadyBooked =
                                mealBookingRepository
                                        .existsByUserAndBookingDate(user, tomorrow);

                        if (!alreadyBooked) {
                            pushNotificationService
                                    .sendMealReminder(user.getId(), tomorrow);
                        }
                    });
                });
    }
}
