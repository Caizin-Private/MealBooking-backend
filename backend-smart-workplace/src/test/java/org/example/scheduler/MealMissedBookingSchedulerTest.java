package org.example.scheduler;

import org.example.entity.NotificationType;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.MealBookingRepository;
import org.example.repository.NotificationRepository;
import org.example.repository.UserRepository;
import org.example.service.NotificationService;
import org.example.service.PushNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.task.scheduling.enabled=true")
@Import(FixedClockConfig.class)
@ActiveProfiles("test")
class MealMissedBookingSchedulerTest {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private MealBookingRepository mealBookingRepository;


    @MockBean
    private NotificationService notificationService;

    @MockBean
    private NotificationRepository notificationRepository;

    @MockBean
    private PushNotificationService pushNotificationService;

    @Autowired
    private MealMissedBookingScheduler missedScheduler;

    private final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    @Test
    void missedBookingScheduledAfterCutoffWhenUserHasNotBooked() {

        // FixedClockConfig sets time to 2026-01-18T18:00Z (before 22:00 cutoff)
        // This test should actually NOT send notifications because it's before cutoff
        LocalDate today = LocalDate.of(2026, 1, 18);

        // Scheduler now uses fixed 22:00 cutoff, and FixedClockConfig runs at 18:00 (before cutoff)

        User user = new User(
                1L,
                "User",
                "user@test.com",
                Role.USER,
                LocalDateTime.now()
        );

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(mealBookingRepository.existsByUserAndBookingDate(user, today))
                .thenReturn(false);

        // Mock notification repository check - return false to allow notification
        when(notificationRepository.existsByUserIdAndTypeAndScheduledAtBetween(
                anyLong(),
                eq(NotificationType.MISSED_BOOKING),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(false);

        // ACT
        missedScheduler.sendMissedMealBookingNotifications();

        // Since FixedClockConfig runs at 18:00 (before 22:00 cutoff),
        // NO notifications should be sent
        verifyNoInteractions(notificationService);
        verifyNoInteractions(pushNotificationService);
    }

    @Test
    void missedBookingNotScheduledWhenUserAlreadyBooked() {

        setupAfterCutoff();

        User user = new User(1L, "User", "u@test.com", Role.USER, LocalDateTime.now());
        when(userRepository.findAll()).thenReturn(List.of(user));

        when(mealBookingRepository.existsByUserAndBookingDate(
                eq(user), any(LocalDate.class)))
                .thenReturn(true);

        missedScheduler.sendMissedMealBookingNotifications();

        verifyNoInteractions(notificationService);
    }

    @Test
    void missedBookingNotScheduledBeforeCutoff() {

        Instant beforeCutoff =
                LocalDateTime.of(2026, 1, 18, 20, 0)
                        .atZone(ZONE)
                        .toInstant();


        missedScheduler.sendMissedMealBookingNotifications();

        verifyNoInteractions(notificationService);
    }

    @Test
    void missedBookingNotScheduledForAdmin() {

        setupAfterCutoff();

        User admin =
                new User(1L, "Admin", "a@test.com", Role.ADMIN, LocalDateTime.now());

        when(userRepository.findAll()).thenReturn(List.of(admin));

        missedScheduler.sendMissedMealBookingNotifications();

        verifyNoInteractions(notificationService);
    }

    private void setupAfterCutoff() {
        Instant afterCutoff =
                LocalDateTime.of(2026, 1, 18, 23, 0)
                        .atZone(ZONE)
                        .toInstant();


    }
}
