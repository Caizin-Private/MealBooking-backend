package org.example.scheduler;

import org.example.entity.NotificationType;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.CutoffConfigRepository;
import org.example.repository.MealBookingRepository;
import org.example.repository.NotificationRepository;
import org.example.repository.UserRepository;
import org.example.service.PushNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.*;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
class MealInactivitySchedulerTest {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private MealBookingRepository mealBookingRepository;

    @MockBean
    private PushNotificationService pushNotificationService;

    @MockBean
    private CutoffConfigRepository cutoffConfigRepository;

    @MockBean
    private Clock clock;

    @MockBean
    private NotificationRepository notificationRepository;

    @Autowired
    private MealInactivityScheduler inactivityScheduler;


    private final ZoneId ZONE = ZoneId.of("UTC");

    @Test
    void inactivityNudgeSentWhenUserHasNoBookingsInLast3Days() {
        // ARRANGE
        Instant now =
                LocalDateTime.of(2026, 1, 18, 10, 0)
                        .atZone(ZONE)
                        .toInstant();

        when(clock.instant()).thenReturn(now);
        when(clock.getZone()).thenReturn(ZONE);

        User user = new User(
                1L,
                "User",
                "user@test.com",
                Role.USER,
                LocalDateTime.now()
        );

        when(userRepository.findAll())
                .thenReturn(List.of(user));

        when(mealBookingRepository
                .existsByUserAndBookingDateBetween(
                        eq(user),
                        eq(LocalDate.of(2026, 1, 15)),
                        eq(LocalDate.of(2026, 1, 17))
                ))
                .thenReturn(false);

        // ACT
        inactivityScheduler.sendInactivityNudges();

        // ASSERT
        verify(pushNotificationService, times(1))
                .sendInactivityNudge(user.getId());
    }

    @Test
    void inactivityNudgeNotSentWhenUserBookedRecently() {

        Instant now =
                LocalDateTime.of(2026, 1, 18, 10, 0)
                        .atZone(ZONE)
                        .toInstant();

        when(clock.instant()).thenReturn(now);
        when(clock.getZone()).thenReturn(ZONE);

        User user = new User(
                1L,
                "User",
                "user@test.com",
                Role.USER,
                LocalDateTime.now()
        );

        when(userRepository.findAll())
                .thenReturn(List.of(user));

        when(mealBookingRepository.existsByUserAndBookingDateBetween(
                eq(user),
                eq(LocalDate.of(2026, 1, 15)),
                eq(LocalDate.of(2026, 1, 17))
        )).thenReturn(true); // user booked recently

        inactivityScheduler.sendInactivityNudges();

        verify(pushNotificationService, never())
                .sendInactivityNudge(anyLong());
    }


    @Test
    void inactivityNudgeNotSentForAdminUser() {

        Instant now =
                LocalDateTime.of(2026, 1, 18, 10, 0)
                        .atZone(ZONE)
                        .toInstant();

        when(clock.instant()).thenReturn(now);
        when(clock.getZone()).thenReturn(ZONE);

        User admin = new User(
                1L,
                "Admin",
                "admin@test.com",
                Role.ADMIN,
                LocalDateTime.now()
        );

        when(userRepository.findAll())
                .thenReturn(List.of(admin));

        inactivityScheduler.sendInactivityNudges();

        verify(pushNotificationService, never())
                .sendInactivityNudge(anyLong());
    }

    @Test
    void inactivityNudgeNotSentWhenNoUsersExist() {

        Instant now =
                LocalDateTime.of(2026, 1, 18, 10, 0)
                        .atZone(ZONE)
                        .toInstant();

        when(clock.instant()).thenReturn(now);
        when(clock.getZone()).thenReturn(ZONE);

        when(userRepository.findAll())
                .thenReturn(List.of());

        inactivityScheduler.sendInactivityNudges();

        verifyNoInteractions(pushNotificationService);
    }


    @Test
    void inactivityNudgeNotSentWhenAlreadyNotifiedToday() {

        Instant now =
                LocalDateTime.of(2026, 1, 18, 10, 0)
                        .atZone(ZONE)
                        .toInstant();

        when(clock.instant()).thenReturn(now);
        when(clock.getZone()).thenReturn(ZONE);

        User user = new User(
                1L,
                "User",
                "user@test.com",
                Role.USER,
                LocalDateTime.now()
        );

        when(userRepository.findAll())
                .thenReturn(List.of(user));

        when(mealBookingRepository.existsByUserAndBookingDateBetween(
                eq(user),
                eq(LocalDate.of(2026, 1, 15)),
                eq(LocalDate.of(2026, 1, 17))
        )).thenReturn(false);

        when(notificationRepository.existsByUserIdAndTypeAndScheduledAtBetween(
                eq(user.getId()),
                eq(NotificationType.INACTIVITY_NUDGE),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(true); // already nudged

        inactivityScheduler.sendInactivityNudges();

        verify(pushNotificationService, never())
                .sendInactivityNudge(anyLong());
    }





}
