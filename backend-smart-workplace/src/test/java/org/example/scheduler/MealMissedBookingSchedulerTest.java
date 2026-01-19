package org.example.scheduler;

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
class MealMissedBookingSchedulerTest {

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
    private MealMissedBookingScheduler missedScheduler;


    private final ZoneId ZONE = ZoneId.of("UTC");

    @Test
    void missedBookingSentAfterCutoffWhenUserHasNotBooked() {

        // ---------- ARRANGE ----------
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

        when(userRepository.findAll())
                .thenReturn(List.of(user));

        when(mealBookingRepository.existsByUserAndBookingDate(user, today))
                .thenReturn(false);

        // ---------- ACT ----------
        missedScheduler.sendMissedMealBookingNotifications();

        // ---------- ASSERT ----------
        verify(pushNotificationService, times(1))
                .sendMissedBookingNotification(user.getId(), today);
    }



}
