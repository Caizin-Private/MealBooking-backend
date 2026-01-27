package org.example.scheduler;

import org.example.entity.Notification;
import org.example.entity.NotificationType;
import org.example.repository.NotificationRepository;
import org.example.service.NotificationService;
import org.example.service.PushNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.task.scheduling.enabled=true")
@Import(FixedClockConfig.class)
@ActiveProfiles("test")
class NotificationSenderSchedulerTest {

    @MockBean
    private NotificationRepository notificationRepository;

    @MockBean
    private PushNotificationService pushNotificationService;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private NotificationSenderScheduler scheduler;

    @Test
    void sendsMealReminderNotification() {
        // ARRANGE - FixedClockConfig provides 2026-01-18T18:00 in Asia/Kolkata
        LocalDateTime now = LocalDateTime.of(2026, 1, 18, 18, 0);

        Notification notification = Notification.builder()
                .id(1L)
                .userId(10L)
                .type(NotificationType.MEAL_REMINDER)
                .scheduledAt(now.minusMinutes(5))
                .sent(false)
                .build();

        when(notificationRepository.findBySentFalseAndScheduledAtBefore(now))
                .thenReturn(List.of(notification));

        // ACT
        scheduler.sendPendingNotifications();

        // ASSERT
        verify(pushNotificationService, times(1))
                .sendMealReminder(10L, notification.getScheduledAt().toLocalDate());

        assertTrue(notification.isSent());
        assertNotNull(notification.getSentAt());

        verify(notificationRepository, times(1))
                .save(notification);
    }


    @Test
    void sendsInactivityNudgeNotification() {
        // ARRANGE - FixedClockConfig provides 2026-01-18T18:00 in Asia/Kolkata
        LocalDateTime now = LocalDateTime.of(2026, 1, 18, 18, 0);

        Notification notification = Notification.builder()
                .userId(7L)
                .type(NotificationType.INACTIVITY_NUDGE)
                .scheduledAt(now.minusDays(1))
                .sent(false)
                .build();

        when(notificationRepository.findBySentFalseAndScheduledAtBefore(now))
                .thenReturn(List.of(notification));

        scheduler.sendPendingNotifications();

        verify(pushNotificationService, times(1))
                .sendInactivityNudge(7L);
    }

    @Test
    void doesNothingWhenNoPendingNotifications() {
        // ARRANGE - FixedClockConfig provides time
        when(notificationRepository.findBySentFalseAndScheduledAtBefore(any()))
                .thenReturn(List.of());

        scheduler.sendPendingNotifications();

        verifyNoInteractions(pushNotificationService);
        verify(notificationRepository, never()).save(any());
    }


}
