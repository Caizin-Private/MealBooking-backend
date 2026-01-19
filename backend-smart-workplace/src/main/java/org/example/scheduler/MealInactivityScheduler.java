package org.example.scheduler;
import lombok.RequiredArgsConstructor;
import org.example.entity.Role;
import org.example.repository.MealBookingRepository;
import org.example.repository.UserRepository;
import org.example.service.PushNotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class MealInactivityScheduler {

    private final UserRepository userRepository;
    private final MealBookingRepository mealBookingRepository;
    private final PushNotificationService pushNotificationService;
    private final Clock clock;

    // 💤 Runs every day at 9 AM
    @Scheduled(cron = "0 0 9 * * *")
    public void sendInactivityNudges() {

        LocalDate cutoffDate = LocalDate.now(clock).minusDays(3);

        userRepository.findAll().forEach(user -> {

            if (user.getRole() != Role.USER) return;

            boolean hasRecentBooking =
                    mealBookingRepository
                            .existsByUserAndBookingDateAfter(user, cutoffDate);

            if (!hasRecentBooking) {
                pushNotificationService.sendInactivityNudge(user.getId());
            }
        });
    }
}
