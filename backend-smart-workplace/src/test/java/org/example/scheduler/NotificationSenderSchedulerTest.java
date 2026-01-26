package org.example.scheduler;

import org.example.entity.Notification;
import org.example.entity.NotificationType;
import org.example.repository.NotificationRepository;
import org.example.service.PushNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.task.scheduling.enabled=true")
@Import(FixedClockConfig.class)
@ActiveProfiles("test")
class NotificationSenderSchedulerTest {

    @MockBean
    private NotificationRepository notificationRepository;

    @MockBean
    private PushNotificationService pushNotificationService;

    @Autowired
    private NotificationSenderScheduler scheduler;

    @Test
    void sendsMealReminderNotification() {

        Notification notification = Notification.builder()
                .id(1L)
                .userId(10L)
                .type(NotificationType.MEAL_REMINDER)
                .scheduledAt(LocalDateTime.now().minusMinutes(5))
                .sent(false)
                .build();

        when(notificationRepository.findBySentFalseAndScheduledAtBefore(any()))
                .thenReturn(List.of(notification));

        scheduler.sendPendingNotifications();

        verify(pushNotificationService)
                .sendMealReminder(eq(10L), eq(notification.getScheduledAt().toLocalDate()));

        verify(notificationRepository).save(notification);
    }

    @Test
    void sendsInactivityNudgeNotification() {

        Notification notification = Notification.builder()
                .id(2L)
                .userId(7L)
                .type(NotificationType.INACTIVITY_NUDGE)
                .scheduledAt(LocalDateTime.now().minusHours(1))
                .sent(false)
                .build();

        when(notificationRepository.findBySentFalseAndScheduledAtBefore(any()))
                .thenReturn(List.of(notification));

        scheduler.sendPendingNotifications();

        verify(pushNotificationService).sendInactivityNudge(7L);
        verify(notificationRepository).save(notification);
    }

    @Test
    void doesNothingWhenNoPendingNotifications() {

        when(notificationRepository.findBySentFalseAndScheduledAtBefore(any()))
                .thenReturn(List.of());

        scheduler.sendPendingNotifications();

        verifyNoInteractions(pushNotificationService);
        verify(notificationRepository, never()).save(any());
    }
}
