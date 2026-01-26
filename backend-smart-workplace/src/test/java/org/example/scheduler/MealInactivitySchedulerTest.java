package org.example.scheduler;

import org.example.config.TestSecurityConfig;
import org.example.entity.NotificationType;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.MealBookingRepository;
import org.example.repository.NotificationRepository;
import org.example.repository.UserRepository;
import org.example.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.*;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.task.scheduling.enabled=true")
@Import({FixedClockConfig.class, TestSecurityConfig.class})
@ActiveProfiles("test")
class MealInactivitySchedulerTest {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private MealBookingRepository mealBookingRepository;

    @MockBean
    private NotificationRepository notificationRepository;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private MealInactivityScheduler inactivityScheduler;

    private final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    /**
     * FixedClockConfig MUST return:
     * 2026-01-16T10:00Z  (FRIDAY)
     */
    @Test
    void inactivityNudgeScheduledWhenUserHasNoBookingsInLast3Days() {

        // GIVEN
        User user = new User(
                1L,
                "User",
                "user@test.com",
                Role.USER,
                LocalDateTime.now()
        );

        when(userRepository.findAll()).thenReturn(List.of(user));

        // Scheduler logic:
        // today = 2026-01-16
        // from  = 2026-01-13
        // to    = 2026-01-15
        when(mealBookingRepository.existsByUserAndBookingDateBetween(
                eq(user),
                eq(LocalDate.of(2026, 1, 13)),
                eq(LocalDate.of(2026, 1, 15))
        )).thenReturn(false);

        when(notificationRepository.existsByUserIdAndTypeAndScheduledAtBetween(
                eq(1L),
                eq(NotificationType.INACTIVITY_NUDGE),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(false);

        // WHEN
        inactivityScheduler.sendInactivityNudges();

        // THEN
        verify(notificationService, times(1)).schedule(
                eq(1L),
                eq("We miss you!"),
                eq("You haven’t booked meals in the last few days."),
                eq(NotificationType.INACTIVITY_NUDGE),
                any(LocalDateTime.class)
        );
    }

    @Test
    void inactivityNudgeNotScheduledWhenUserBookedRecently() {

        User user = new User(
                1L,
                "User",
                "u@test.com",
                Role.USER,
                LocalDateTime.now()
        );

        when(userRepository.findAll()).thenReturn(List.of(user));

        when(mealBookingRepository.existsByUserAndBookingDateBetween(
                any(), any(), any()
        )).thenReturn(true);

        inactivityScheduler.sendInactivityNudges();

        verifyNoInteractions(notificationService);
    }

    @Test
    void inactivityNudgeNotScheduledForAdminUser() {

        User admin = new User(
                1L,
                "Admin",
                "a@test.com",
                Role.ADMIN,
                LocalDateTime.now()
        );

        when(userRepository.findAll()).thenReturn(List.of(admin));

        inactivityScheduler.sendInactivityNudges();

        verifyNoInteractions(notificationService);
    }

    @Test
    void inactivityNudgeNotScheduledWhenNoUsersExist() {

        when(userRepository.findAll()).thenReturn(List.of());

        inactivityScheduler.sendInactivityNudges();

        verifyNoInteractions(notificationService);
    }
}
