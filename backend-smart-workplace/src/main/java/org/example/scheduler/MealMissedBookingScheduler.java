package org.example.scheduler;
import lombok.RequiredArgsConstructor;
import org.example.entity.CutoffConfig;
import org.example.entity.Role;
import org.example.entity.User;
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
public class MealMissedBookingScheduler {

    private final UserRepository userRepository;
    private final MealBookingRepository mealBookingRepository;
    private final PushNotificationService pushNotificationService;
    private final CutoffConfigRepository cutoffConfigRepository;
    private final Clock clock;

    //Runs AFTER cutoff (10:30 PM)
    @Scheduled(cron = "0 30 22 * * *")
    public void sendMissedMealBookingNotifications() {

        cutoffConfigRepository.findTopByOrderByIdDesc()
                .ifPresent(cutoffConfig -> {

                    LocalTime now = LocalTime.now(clock);

                    if (now.isBefore(cutoffConfig.getCutoffTime())) {
                        return;
                    }

                    LocalDate today = LocalDate.now(clock);

                    userRepository.findAll().forEach(user -> {

                        if (user.getRole() != Role.USER) return;

                        boolean booked =
                                mealBookingRepository
                                        .existsByUserAndBookingDate(user, today);

                        if (!booked) {
                            pushNotificationService
                                    .sendMissedBookingNotification(user.getId(), today);
                        }
                    });
                });
    }
}
