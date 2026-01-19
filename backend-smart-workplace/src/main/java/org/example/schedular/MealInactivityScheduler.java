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
public class MealInactivityScheduler {

    private final UserRepository userRepository;
    private final MealBookingRepository mealBookingRepository;
    private final PushNotificationService pushNotificationService;
    private final Clock clock;

    public void sendInactivityNudges() {
        // empty for now
    }
}
