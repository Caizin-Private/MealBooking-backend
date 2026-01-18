package org.example.serviceTests;

import org.example.entity.CutoffConfig;
import org.example.entity.MealBooking;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.CutoffConfigRepository;
import org.example.repository.MealBookingRepository;
import org.example.service.GeoFenceService;
import org.example.service.MealBookingService;
import org.example.service.PushNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.junit.jupiter.api.Assertions.assertThrows;


import java.time.*;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
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
        // Default system time: 12:00 noon (before cutoff)
        Instant fixedInstant =
                LocalDateTime.of(2026, 1, 18, 12, 0)
                        .atZone(ZONE)
                        .toInstant();

        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(ZONE);

        when(cutoffConfigRepository.findAll())
                .thenReturn(
                        List.of(
                                CutoffConfig.builder()
                                        .cutoffTime(LocalTime.of(22, 0))
                                        .build()
                        )
                );
    }

    @Test
    void userCanBookFutureDate() {
        User user = new User(1L, "Test User", "test@test.com", Role.USER, LocalDateTime.now(clock));

        LocalDate bookingDate = LocalDate.now(clock).plusDays(2);

        when(geoFenceService.isInsideAllowedArea(anyDouble(), anyDouble()))
                .thenReturn(true);

        mealBookingService.bookMeals(user, List.of(bookingDate), 10.0, 10.0);

        verify(mealBookingRepository).save(any(MealBooking.class));
    }

    @Test
    void adminCannotBookMeals() {
        User admin = new User(2L, "Admin", "admin@test.com", Role.ADMIN, LocalDateTime.now(clock));

        assertThrows(RuntimeException.class,
                () -> mealBookingService.bookMeals(
                        admin,
                        List.of(LocalDate.now(clock).plusDays(1)),
                        10.0,
                        10.0
                )
        );
    }

    @Test
    void bookingFailsWhenOutsideGeofence() {
        User user = new User(1L, "User", "user@test.com", Role.USER, LocalDateTime.now(clock));

        when(geoFenceService.isInsideAllowedArea(anyDouble(), anyDouble()))
                .thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> mealBookingService.bookMeals(
                        user,
                        List.of(LocalDate.now(clock).plusDays(2)),
                        0.0,
                        0.0
                )
        );
    }

    @Test
    void bookingForTomorrowFailsAfterCutoff() {
        // Simulate AFTER cutoff time (23:00)
        Instant afterCutoff =
                LocalDateTime.of(2026, 1, 18, 23, 0)
                        .atZone(ZONE)
                        .toInstant();

        when(clock.instant()).thenReturn(afterCutoff);

        User user = new User(1L, "User", "user@test.com", Role.USER, LocalDateTime.now(clock));

        when(geoFenceService.isInsideAllowedArea(anyDouble(), anyDouble()))
                .thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> mealBookingService.bookMeals(
                        user,
                        List.of(LocalDate.now(clock).plusDays(1)),
                        10.0,
                        10.0
                )
        );
    }

    @Test
    void duplicateBookingFails() {
        User user = new User(1L, "User", "user@test.com", Role.USER, LocalDateTime.now(clock));

        LocalDate bookingDate = LocalDate.now(clock).plusDays(2);

        when(geoFenceService.isInsideAllowedArea(anyDouble(), anyDouble()))
                .thenReturn(true);

        when(mealBookingRepository.existsByUserIdAndBookingDate(user.getId(), bookingDate))
                .thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> mealBookingService.bookMeals(
                        user,
                        List.of(bookingDate),
                        10.0,
                        10.0
                )
        );
    }

    @Test
    void pushNotificationSentAfterSuccessfulBooking() {
        User user = new User(1L, "User", "user@test.com", Role.USER, LocalDateTime.now(clock));

        LocalDate bookingDate = LocalDate.now(clock).plusDays(2);

        when(geoFenceService.isInsideAllowedArea(anyDouble(), anyDouble()))
                .thenReturn(true);

        when(mealBookingRepository.existsByUserIdAndBookingDate(user.getId(), bookingDate))
                .thenReturn(false);

        mealBookingService.bookMeals(user, List.of(bookingDate), 10.0, 10.0);

        verify(pushNotificationService, times(1))
                .sendBookingConfirmation(user.getId(), List.of(bookingDate));
    }

    @Test
    void multipleDatesAreSavedIndividually() {
        User user = new User(1L, "User", "user@test.com", Role.USER, LocalDateTime.now(clock));

        List<LocalDate> dates = List.of(
                LocalDate.now(clock).plusDays(2),
                LocalDate.now(clock).plusDays(3),
                LocalDate.now(clock).plusDays(5)
        );

        when(geoFenceService.isInsideAllowedArea(anyDouble(), anyDouble()))
                .thenReturn(true);

        when(mealBookingRepository.existsByUserIdAndBookingDate(anyLong(), any(LocalDate.class)))
                .thenReturn(false);

        mealBookingService.bookMeals(user, dates, 10.0, 10.0);

        verify(mealBookingRepository, times(3)).save(any(MealBooking.class));
    }
}
