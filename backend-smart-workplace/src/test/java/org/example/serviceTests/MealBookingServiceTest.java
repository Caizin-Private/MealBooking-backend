package org.example.serviceTests;

import org.example.entity.MealBooking;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.MealBookingRepository;
import org.example.service.GeoFenceService;
import org.example.service.MealBookingService;
import org.example.service.PushNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void userCanBookFutureDate() {
        // GIVEN
        User user = new User(
                1L,
                "Test User",
                "test@test.com",
                Role.USER,
                LocalDateTime.now()
        );

        LocalDate bookingDate = LocalDate.now().plusDays(2);

        when(geoFenceService.isInsideAllowedArea(anyDouble(), anyDouble()))
                .thenReturn(true);

        // WHEN
        mealBookingService.bookMeals(
                user,
                List.of(bookingDate),
                10.0,
                10.0
        );

        // THEN
        verify(mealBookingRepository).save(any(MealBooking.class));
    }

    @Test
    void adminCannotBookMeals() {
        User admin = new User(
                2L,
                "Admin User",
                "admin@test.com",
                Role.ADMIN,
                LocalDateTime.now()
        );

        RuntimeException exception = org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> mealBookingService.bookMeals(
                        admin,
                        List.of(LocalDate.now().plusDays(1)),
                        10.0,
                        10.0
                )
        );
    }

    @Test
    void bookingFailsWhenOutsideGeofence() {
        User user = new User(
                1L,
                "User",
                "user@test.com",
                Role.USER,
                LocalDateTime.now()
        );

        // GeoFence denies location
        when(geoFenceService.isInsideAllowedArea(anyDouble(), anyDouble()))
                .thenReturn(false);

        RuntimeException exception = org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> mealBookingService.bookMeals(
                        user,
                        List.of(LocalDate.now().plusDays(2)),
                        0.0,
                        0.0
                )
        );
    }


}
