package org.example.service;

import org.example.entity.CutoffConfig;
import org.example.entity.MealBooking;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.CutoffConfigRepository;
import org.example.repository.MealBookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.assertThrows;


import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class MealBookingServiceTest {

    @Autowired
    private MealBookingService mealBookingService;

    @MockBean
    private MealBookingRepository mealBookingRepository;

    @MockBean
    private GeoFenceService geoFenceService;

    @MockBean
    private PushNotificationService pushNotificationService;

    @MockBean
    private CutoffConfigRepository cutoffConfigRepository;

    @MockBean
    private Clock clock;

    private final ZoneId ZONE = ZoneId.of("UTC");

    @BeforeEach
    void setup() {
        Instant fixedInstant =
                LocalDateTime.of(2026, 1, 18, 12, 0)
                        .atZone(ZONE)
                        .toInstant();

        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(ZONE);

        when(cutoffConfigRepository.findTopByOrderByIdDesc())
                .thenReturn(
                        java.util.Optional.of(
                                CutoffConfig.builder()
                                        .cutoffTime(LocalTime.of(22, 0))
                                        .build()
                        )
                );
    }

    @Test
    void userCanBookFutureDates() {
        User user = new User(1L, "User", "user@test.com", Role.USER, LocalDateTime.now(clock));

        LocalDate start = LocalDate.now(clock).plusDays(2);
        LocalDate end = start.plusDays(2);

        when(geoFenceService.isInsideAllowedArea(anyDouble(), anyDouble()))
                .thenReturn(true);

        when(mealBookingRepository.existsByUserAndBookingDate(
                any(User.class),
                any(LocalDate.class)
        )).thenReturn(false);

        mealBookingService.bookMeals(user, start, end, 10.0, 10.0);

        verify(mealBookingRepository, times(3)).save(any(MealBooking.class));
    }

    @Test
    void bookingFailsWhenOutsideGeofence() {
        User user = new User(1L, "User", "user@test.com", Role.USER, LocalDateTime.now(clock));

        when(geoFenceService.isInsideAllowedArea(anyDouble(), anyDouble()))
                .thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> mealBookingService.bookMeals(
                        user,
                        LocalDate.now(clock).plusDays(2),
                        LocalDate.now(clock).plusDays(3),
                        0.0,
                        0.0
                )
        );
    }

    @Test
    void bookingForTomorrowFailsAfterCutoff() {

        Instant afterCutoff =
                LocalDateTime.of(2026, 1, 18, 23, 0)
                        .atZone(ZONE)
                        .toInstant();

        // 🔑 FIX
        when(clock.instant()).thenReturn(afterCutoff);
        when(clock.getZone()).thenReturn(ZONE);

        User user = new User(
                1L, "User", "user@test.com", Role.USER, LocalDateTime.now(clock)
        );

        when(geoFenceService.isInsideAllowedArea(anyDouble(), anyDouble()))
                .thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> mealBookingService.bookMeals(
                        user,
                        LocalDate.now(clock).plusDays(1),
                        LocalDate.now(clock).plusDays(1),
                        10.0,
                        10.0
                )
        );
    }


    @Test
    void duplicateBookingFails() {
        User user = new User(1L, "User", "user@test.com", Role.USER, LocalDateTime.now(clock));

        LocalDate date = LocalDate.now(clock).plusDays(2);

        when(geoFenceService.isInsideAllowedArea(anyDouble(), anyDouble()))
                .thenReturn(true);

        when(mealBookingRepository.existsByUserAndBookingDate(
                any(User.class),
                any(LocalDate.class)
        )).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> mealBookingService.bookMeals(
                        user,
                        date,
                        date,
                        10.0,
                        10.0
                )
        );
    }

    @Test
    void pushNotificationSentAfterSuccessfulBooking() {
        User user = new User(1L, "User", "user@test.com", Role.USER, LocalDateTime.now(clock));

        LocalDate start = LocalDate.now(clock).plusDays(2);
        LocalDate end = start.plusDays(1);

        when(geoFenceService.isInsideAllowedArea(anyDouble(), anyDouble()))
                .thenReturn(true);

        when(mealBookingRepository.existsByUserAndBookingDate(
                any(User.class),
                any(LocalDate.class)
        )).thenReturn(false);

        mealBookingService.bookMeals(user, start, end, 10.0, 10.0);

        verify(pushNotificationService, times(1))
                .sendBookingConfirmation(
                        eq(user.getId()),
                        eq(start),
                        eq(end)
                );

    }

    @Test
    void multipleDatesAreSavedIndividually() {
        User user = new User(1L, "User", "user@test.com", Role.USER, LocalDateTime.now(clock));

        LocalDate start = LocalDate.now(clock).plusDays(2);
        LocalDate end = start.plusDays(3); // 4 days total

        when(geoFenceService.isInsideAllowedArea(anyDouble(), anyDouble()))
                .thenReturn(true);

        when(mealBookingRepository.existsByUserAndBookingDate(
                any(User.class),
                any(LocalDate.class)
        )).thenReturn(false);

        mealBookingService.bookMeals(user, start, end, 10.0, 10.0);

        verify(mealBookingRepository, times(4)).save(any(MealBooking.class));
    }

    @Test
    void cancelBookingDeletesRecordAndSendsNotification() {

        User user = new User(1L, "User", "user@test.com", Role.USER, LocalDateTime.now(clock));
        LocalDate date = LocalDate.now(clock).plusDays(2);

        MealBooking booking = MealBooking.builder()
                .user(user)
                .bookingDate(date)
                .build();

        when(mealBookingRepository.findByUserAndBookingDate(user, date))
                .thenReturn(Optional.of(booking));

        mealBookingService.cancelMeal(user, date);

        verify(mealBookingRepository).delete(booking);
        verify(pushNotificationService)
                .sendCancellationConfirmation(user.getId(), date);
    }

}
