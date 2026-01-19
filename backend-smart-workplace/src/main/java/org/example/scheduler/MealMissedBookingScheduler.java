package org.example.scheduler;
import lombok.RequiredArgsConstructor;
import org.example.entity.CutoffConfig;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.CutoffConfigRepository;
import org.example.repository.MealBookingRepository;
import org.example.repository.UserRepository;
import org.example.service.PushNotificationService;
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

    public void sendMissedMealBookingNotifications() {

        Optional<CutoffConfig> cutoffOpt =
                cutoffConfigRepository.findTopByOrderByIdDesc();

        if (cutoffOpt.isEmpty()) {
            return; // test expects no crash
        }

        CutoffConfig cutoffConfig = cutoffOpt.get();
        LocalTime now = LocalTime.now(clock);

        // Missed booking only AFTER cutoff
        if (now.isBefore(cutoffConfig.getCutoffTime())) {
            return;
        }

        LocalDate today = LocalDate.now(clock);

        for (User user : userRepository.findAll()) {

            if (user.getRole() != Role.USER) {
                continue;
            }

            boolean booked =
                    mealBookingRepository.existsByUserAndBookingDate(user, today);

            if (!booked) {
                pushNotificationService.sendMissedBookingNotification(
                        user.getId(),
                        today
                );
            }
        }
    }
}