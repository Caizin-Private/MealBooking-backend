package org.example.schedular;


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
public class MealReminderScheduler {

    private final UserRepository userRepository;
    private final MealBookingRepository mealBookingRepository;
    private final PushNotificationService pushNotificationService;
    private final CutoffConfigRepository cutoffConfigRepository;
    private final Clock clock;

    public void sendMealBookingReminders() {

        Optional<CutoffConfig> cutoffOpt =
                cutoffConfigRepository.findTopByOrderByIdDesc();

        if (cutoffOpt.isEmpty()) {
            return; // no config → no reminders
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

            if (!alreadyBooked) {
                pushNotificationService.sendMealReminder(
                        user.getId(),
                        tomorrow
                );
            }
        });
    }

}
