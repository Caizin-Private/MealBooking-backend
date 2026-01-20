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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@Import(FixedClockConfig.class)
@ActiveProfiles("test")
class MealMissedBookingSchedulerTest {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private MealBookingRepository mealBookingRepository;

    @MockBean
    private CutoffConfigRepository cutoffConfigRepository;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private Clock clock;

    @Autowired
    private MealMissedBookingScheduler missedScheduler;

    private final ZoneId ZONE = ZoneId.of("UTC");

    @Test
    void missedBookingScheduledAfterCutoffWhenUserHasNotBooked() {

        // ARRANGE
        Instant afterCutoff =
                LocalDateTime.of(2026, 1, 18, 23, 0)
                        .atZone(ZONE)
                        .toInstant();

        when(clock.instant()).thenReturn(afterCutoff);
        when(clock.getZone()).thenReturn(ZONE);

        LocalDate today = LocalDate.of(2026, 1, 18);

        when(cutoffConfigRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.of(
                        CutoffConfig.builder()
                                .cutoffTime(LocalTime.of(22, 0))
                                .build()
                ));

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

        // ACT
        missedScheduler.sendMissedMealBookingNotifications();

        // ASSERT
        verify(notificationService, times(1))
                .schedule(
                        eq(user.getId()),
                        eq("Meal booking missed"),
                        eq("You missed booking your meal for today."),
                        eq(NotificationType.MISSED_BOOKING),
                        eq(today.atStartOfDay())
                );
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

        when(clock.instant()).thenReturn(beforeCutoff);
        when(clock.getZone()).thenReturn(ZONE);

        when(cutoffConfigRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.of(
                        CutoffConfig.builder()
                                .cutoffTime(LocalTime.of(22, 0))
                                .build()
                ));

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

        when(clock.instant()).thenReturn(afterCutoff);
        when(clock.getZone()).thenReturn(ZONE);

        when(cutoffConfigRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.of(
                        CutoffConfig.builder()
                                .cutoffTime(LocalTime.of(22, 0))
                                .build()
                ));
    }
}
