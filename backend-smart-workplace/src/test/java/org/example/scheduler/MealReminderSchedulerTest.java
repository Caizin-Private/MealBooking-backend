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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.task.scheduling.enabled=true")
@Import({MondayClockConfig.class, TestSecurityConfig.class})
@ActiveProfiles("test")
class MealReminderSchedulerWeekdayTest {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private MealBookingRepository mealBookingRepository;

    @MockBean
    private NotificationRepository notificationRepository;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private MealReminderScheduler scheduler;

    @Test
    void reminderSentWhenEligibleUserAndNoBooking() {

        LocalDate tomorrow = LocalDate.of(2026, 1, 20);

        User user = new User(
                1L, "User", "user@test.com", Role.USER, LocalDateTime.now()
        );

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(mealBookingRepository.existsByUserAndBookingDate(user, tomorrow))
                .thenReturn(false);

        when(notificationRepository.existsByUserIdAndTypeAndSentFalse(
                1L, NotificationType.MEAL_REMINDER
        )).thenReturn(false);

        when(notificationRepository.existsByUserIdAndTypeAndScheduledAtBetween(
                any(), any(), any(), any()
        )).thenReturn(false);

        scheduler.sendMealBookingReminders();

        verify(notificationService).schedule(
                eq(1L),
                eq("Meal booking reminder"),
                eq("Please book your meal for " + tomorrow),
                eq(NotificationType.MEAL_REMINDER),
                any()
        );
    }
}

