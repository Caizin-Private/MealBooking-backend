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

    @Autowired
    private MealMissedBookingScheduler missedScheduler;


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




}
