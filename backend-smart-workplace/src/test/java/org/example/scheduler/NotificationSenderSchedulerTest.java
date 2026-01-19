package org.example.scheduler;

import org.example.entity.Notification;
import org.example.entity.NotificationType;
import org.example.repository.NotificationRepository;
import org.example.service.PushNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest(
        properties = "spring.task.scheduling.enabled=false"
)
class NotificationSenderSchedulerTest {

    @MockBean
    private NotificationRepository notificationRepository;

    @MockBean
    private PushNotificationService pushNotificationService;

    @MockBean
    private Clock clock;

    @Autowired
    private NotificationSenderScheduler scheduler;

    @Test
    void sendsMealReminderNotification() {
        ZoneId zone = ZoneId.of("UTC");
        // ARRANGE
        LocalDateTime now = LocalDateTime.of(2026, 1, 18, 18, 0);
        when(clock.instant()).thenReturn(
                now.atZone(ZoneId.systemDefault()).toInstant()
        );
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

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
                .saveAll(anyList());
    }


    @Test
    void sendsMissedBookingNotification() {
        ZoneId zone = ZoneId.of("UTC");

        LocalDateTime now = LocalDateTime.of(2026, 1, 18, 23, 0);
        when(clock.instant()).thenReturn(
                now.atZone(ZoneId.systemDefault()).toInstant()
        );
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        Notification notification = Notification.builder()
                .userId(5L)
                .type(NotificationType.MISSED_BOOKING)
                .scheduledAt(now.minusMinutes(10))
                .sent(false)
                .build();

        when(notificationRepository.findBySentFalseAndScheduledAtBefore(now))
                .thenReturn(List.of(notification));

        scheduler.sendPendingNotifications();

        verify(pushNotificationService, times(1))
                .sendMissedBookingNotification(5L, now.toLocalDate());

        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    void sendsInactivityNudgeNotification() {
        ZoneId zone = ZoneId.of("UTC");

        LocalDateTime now = LocalDateTime.of(2026, 1, 18, 10, 0);
        when(clock.instant()).thenReturn(
                now.atZone(ZoneId.systemDefault()).toInstant()
        );
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

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

        ZoneId zone = ZoneId.of("UTC");
        LocalDateTime now = LocalDateTime.of(2026, 1, 19, 10, 0);

        when(clock.instant()).thenReturn(now.atZone(zone).toInstant());
        when(clock.getZone()).thenReturn(zone);

        when(notificationRepository.findBySentFalseAndScheduledAtBefore(any()))
                .thenReturn(List.of());

        scheduler.sendPendingNotifications();

        verifyNoInteractions(pushNotificationService);
        verify(notificationRepository, never()).saveAll(any());
    }


}
