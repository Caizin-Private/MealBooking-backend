package org.example.scheduler;

import org.example.entity.CutoffConfig;
import org.example.entity.NotificationType;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.CutoffConfigRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.task.scheduling.enabled=true")
@Import(FixedClockConfig.class)   // fixed time: 2026-01-18T18:00Z
@ActiveProfiles("test")
class MealReminderSchedulerTest {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private MealBookingRepository mealBookingRepository;

    @MockBean
    private CutoffConfigRepository cutoffConfigRepository;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private NotificationRepository notificationRepository;

    @MockBean
    private PushNotificationService pushNotificationService;

    @Autowired
    private MealReminderScheduler scheduler;

    @Test
    void reminderSentWhenUserHasNotBookedAndBeforeCutoff() {

        // Tomorrow relative to FixedClockConfig
        LocalDate tomorrow = LocalDate.of(2026, 1, 19);

        // ---- Cutoff (22:00, so 18:00 is BEFORE cutoff)
        when(cutoffConfigRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.of(
                        CutoffConfig.builder()
                                .cutoffTime(LocalTime.of(22, 0))
                                .build()
                ));

        // ---- User
        User user = new User(
                1L,
                "User",
                "user@test.com",
                null,
                null,
                null,
                Role.USER,
                LocalDateTime.now(),
                null
        );

        when(userRepository.findAll()).thenReturn(List.of(user));

        // ---- User has NOT booked
        when(mealBookingRepository.existsByUserAndBookingDate(user, tomorrow))
                .thenReturn(false);

        // ✅ CRITICAL FIX — idempotency check must return FALSE
        when(notificationRepository.existsByUserIdAndTypeAndScheduledAtBetween(
                anyLong(),
                eq(NotificationType.MEAL_REMINDER),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(false);

        // ---- ACT
        scheduler.sendMealBookingReminders();

        // ---- ASSERT
        verify(notificationService, times(1)).schedule(
                eq(1L),
                eq("Meal booking reminder"),
                eq("Please book your meal for 2026-01-19"),
                eq(NotificationType.MEAL_REMINDER),
                any(LocalDateTime.class)
        );

        verify(pushNotificationService, times(1))
                .sendMealReminder(1L, tomorrow);
    }


    @Test
    void reminderNotSentAfterCutoff() {

        when(cutoffConfigRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.of(
                        CutoffConfig.builder()
                                .cutoffTime(LocalTime.of(17, 0))
                                .build()
                ));

        User user = new User(
                1L, "User", "u@test.com", null, null, null, Role.USER, LocalDateTime.now(), null
        );

        when(userRepository.findAll()).thenReturn(List.of(user));

        scheduler.sendMealBookingReminders();

        verifyNoInteractions(notificationService);
        verifyNoInteractions(pushNotificationService);
    }

    @Test
    void reminderNotSentWhenUserAlreadyBooked() {

        LocalDate tomorrow = LocalDate.of(2026, 1, 19);

        when(cutoffConfigRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.of(
                        CutoffConfig.builder()
                                .cutoffTime(LocalTime.of(22, 0))
                                .build()
                ));

        User user = new User(
                1L, "User", "u@test.com", null, null, null, Role.USER, LocalDateTime.now(), null
        );

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(mealBookingRepository.existsByUserAndBookingDate(user, tomorrow))
                .thenReturn(true);

        scheduler.sendMealBookingReminders();

        verifyNoInteractions(notificationService);
        verifyNoInteractions(pushNotificationService);
    }

    @Test
    void reminderNotSentWhenCutoffConfigMissing() {

        when(cutoffConfigRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.empty());

        scheduler.sendMealBookingReminders();

        verifyNoInteractions(notificationService);
        verifyNoInteractions(pushNotificationService);
    }

    @Test
    void reminderNotSentForAdminUsers() {

        when(cutoffConfigRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.of(
                        CutoffConfig.builder()
                                .cutoffTime(LocalTime.of(22, 0))
                                .build()
                ));

        User admin = new User(
                1L, "Admin", "admin@test.com", null, null, null, Role.ADMIN, LocalDateTime.now(), null
        );

        when(userRepository.findAll()).thenReturn(List.of(admin));

        scheduler.sendMealBookingReminders();

        verifyNoInteractions(notificationService);
        verifyNoInteractions(pushNotificationService);
    }
}
