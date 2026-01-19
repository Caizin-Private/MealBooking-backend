package org.example.schedular;

import org.example.entity.CutoffConfig;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.CutoffConfigRepository;
import org.example.repository.MealBookingRepository;
import org.example.repository.UserRepository;
import org.example.service.PushNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
class MealReminderSchedulerTest {

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

    @Autowired
    private MealReminderScheduler scheduler;

    private final ZoneId ZONE = ZoneId.of("UTC");

    @Test
    void reminderSentWhenUserHasNotBookedAndBeforeCutoff() {

        // ---------- ARRANGE ----------
        Instant fixedInstant =
                LocalDateTime.of(2026, 1, 18, 18, 0)
                        .atZone(ZONE)
                        .toInstant();

        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(ZONE);

        LocalDate today = LocalDate.of(2026, 1, 18);
        LocalDate tomorrow = today.plusDays(1);

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
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );

        when(userRepository.findAll())
                .thenReturn(List.of(user));

        when(mealBookingRepository.existsByUserAndBookingDate(user, tomorrow))
                .thenReturn(false);

        // ---------- ACT ----------
        scheduler.sendMealBookingReminders();

        // ---------- ASSERT ----------
        verify(pushNotificationService, times(1))
                .sendMealReminder(user.getId(), tomorrow);
    }

    @Test
    void reminderNotSentAfterCutoff() {

        // ARRANGE — time after cutoff
        Instant fixedInstant =
                LocalDateTime.of(2026, 1, 18, 23, 0)
                        .atZone(ZONE)
                        .toInstant();

        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(ZONE);

        LocalDate tomorrow = LocalDate.of(2026, 1, 19);

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
        when(mealBookingRepository.existsByUserAndBookingDate(user, tomorrow))
                .thenReturn(false);

        // ACT
        scheduler.sendMealBookingReminders();

        // ASSERT
        verify(pushNotificationService, never())
                .sendMealReminder(anyLong(), any());
    }

    @Test
    void reminderNotSentWhenUserAlreadyBooked() {

        Instant fixedInstant =
                LocalDateTime.of(2026, 1, 18, 18, 0)
                        .atZone(ZONE)
                        .toInstant();

        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(ZONE);

        LocalDate tomorrow = LocalDate.of(2026, 1, 19);

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

        when(mealBookingRepository.existsByUserAndBookingDate(user, tomorrow))
                .thenReturn(true); // already booked

        scheduler.sendMealBookingReminders();

        verify(pushNotificationService, never())
                .sendMealReminder(anyLong(), any());
    }


    @Test
    void reminderNotSentWhenCutoffConfigMissing() {

        when(cutoffConfigRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.empty());

        scheduler.sendMealBookingReminders();

        verifyNoInteractions(pushNotificationService);
    }

    @Test
    void reminderNotSentForAdminUsers() {

        Instant fixedInstant =
                LocalDateTime.of(2026, 1, 18, 18, 0)
                        .atZone(ZONE)
                        .toInstant();

        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(ZONE);

        when(cutoffConfigRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.of(
                        CutoffConfig.builder()
                                .cutoffTime(LocalTime.of(22, 0))
                                .build()
                ));

        User admin = new User(
                1L,
                "Admin",
                "admin@test.com",
                Role.ADMIN,
                LocalDateTime.now()
        );

        when(userRepository.findAll()).thenReturn(List.of(admin));

        scheduler.sendMealBookingReminders();

        verify(pushNotificationService, never())
                .sendMealReminder(anyLong(), any());
    }


}
