package org.example.scheduler;

import org.example.config.TestSecurityConfig;
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
    private PushNotificationService pushNotificationService;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private MealInactivityScheduler inactivityScheduler;

    private final ZoneId ZONE = ZoneId.of("UTC");

    @Test
    void inactivityNudgeScheduledWhenUserHasNoBookingsInLast3Days() {

        Instant now = LocalDateTime.of(2026, 1, 18, 10, 0)
                .atZone(ZONE)
                .toInstant();

        User user = new User(
                1L,
                "User",
                "user@test.com",
                Role.USER,
                LocalDateTime.now()
        );

        when(userRepository.findAll()).thenReturn(List.of(user));

        when(mealBookingRepository.existsByUserAndBookingDateBetween(
                eq(user),
                eq(LocalDate.of(2026, 1, 15)),
                eq(LocalDate.of(2026, 1, 18))
        )).thenReturn(false);

        inactivityScheduler.sendInactivityNudges();

        verify(notificationService, times(1)).schedule(
                eq(user.getId()),
                eq("We miss you!"),
                eq("You haven’t booked meals in the last few days."),
                eq(NotificationType.INACTIVITY_NUDGE),
                any(LocalDateTime.class)
        );
    }

    @Test
    void inactivityNudgeNotScheduledWhenUserBookedRecently() {

        setupClock();

        User user = user(Role.USER);

        when(userRepository.findAll()).thenReturn(List.of(user));

        when(mealBookingRepository.existsByUserAndBookingDateBetween(
                any(), any(), any()
        )).thenReturn(true);

        inactivityScheduler.sendInactivityNudges();

        verifyNoInteractions(notificationService);
    }

    @Test
    void inactivityNudgeNotScheduledForAdminUser() {

        setupClock();

        User admin = user(Role.ADMIN);

        when(userRepository.findAll()).thenReturn(List.of(admin));

        inactivityScheduler.sendInactivityNudges();

        verifyNoInteractions(notificationService);
    }

    @Test
    void inactivityNudgeNotScheduledWhenNoUsersExist() {

        setupClock();

        when(userRepository.findAll()).thenReturn(List.of());

        inactivityScheduler.sendInactivityNudges();

        verifyNoInteractions(notificationService);
    }

    // ---------- HELPERS ----------

    private void setupClock() {
        Instant now = LocalDateTime.of(2026, 1, 18, 10, 0)
                .atZone(ZONE)
                .toInstant();
    }

    private User user(Role role) {
        return new User(
                1L,
                "Test",
                "test@test.com",
                role,
                LocalDateTime.now()
        );
    }
}
