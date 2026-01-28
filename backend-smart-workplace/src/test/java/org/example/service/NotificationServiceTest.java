package org.example.service;

import org.example.entity.Notification;
import org.example.entity.NotificationType;
import org.example.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Clock clock;
    private LocalDateTime testTime;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(LocalDateTime.of(2026, 1, 26, 12, 0).toInstant(ZoneOffset.of("+05:30")), ZoneId.of("Asia/Kolkata"));
        ReflectionTestUtils.setField(notificationService, "clock", clock);

        testTime = LocalDateTime.now(clock);

        testNotification = Notification.builder()
                .id(1L)
                .userId(3L)
                .title("Test Title")
                .message("Test Message")
                .type(NotificationType.MEAL_REMINDER)
                .scheduledAt(testTime)
                .sent(false)
                .build();
    }

    @Test
    void schedule_ValidNotification_ShouldSaveNotification() {
        Long userId = 3L;
        String title = "Meal Reminder";
        String message = "Please book your meal";
        NotificationType type = NotificationType.MEAL_REMINDER;
        LocalDateTime scheduleTime = testTime;

        notificationService.schedule(userId, title, message, type, scheduleTime);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getUserId().equals(userId) &&
                        notification.getTitle().equals(title) &&
                        notification.getMessage().equals(message) &&
                        notification.getType().equals(type) &&
                        notification.getScheduledAt().equals(scheduleTime) &&
                        !notification.isSent() &&
                        notification.getSentAt() == null
        ));
    }

    @Test
    void schedule_AllNotificationTypes_ShouldWork() {
        Long userId = 3L;
        String title = "Test Title";
        String message = "Test Message";
        LocalDateTime scheduleTime = testTime;

        for (NotificationType type : NotificationType.values()) {
            notificationService.schedule(userId, title, message, type, scheduleTime);
        }
        verify(notificationRepository, times(NotificationType.values().length)).save(any(Notification.class));
    }

    @Test
    void schedule_WithNullValues_ShouldStillSave() {
        notificationService.schedule(null, null, null, null, null);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getUserId() == null &&
                        notification.getTitle() == null &&
                        notification.getMessage() == null &&
                        notification.getType() == null &&
                        notification.getScheduledAt() == null &&
                        !notification.isSent()
        ));
    }

    @Test
    void schedule_WithEmptyStrings_ShouldSave() {
        Long userId = 3L;
        String emptyTitle = "";
        String emptyMessage = "";
        NotificationType type = NotificationType.MEAL_REMINDER;
        LocalDateTime scheduleTime = testTime;

        notificationService.schedule(userId, emptyTitle, emptyMessage, type, scheduleTime);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getUserId().equals(userId) &&
                        notification.getTitle().equals(emptyTitle) &&
                        notification.getMessage().equals(emptyMessage) &&
                        notification.getType().equals(type) &&
                        notification.getScheduledAt().equals(scheduleTime) &&
                        !notification.isSent()
        ));
    }

    @Test
    void schedule_WithFutureTime_ShouldSave() {
        LocalDateTime futureTime = testTime.plusDays(1);

        notificationService.schedule(3L, "Future Test", "Future message", NotificationType.MEAL_REMINDER, futureTime);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getScheduledAt().equals(futureTime)
        ));
    }

    @Test
    void schedule_WithPastTime_ShouldSave() {
        LocalDateTime pastTime = testTime.minusHours(1);

        notificationService.schedule(3L, "Past Test", "Past message", NotificationType.MEAL_REMINDER, pastTime);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getScheduledAt().equals(pastTime)
        ));
    }

    @Test
    void markAsSent_ValidNotification_ShouldUpdateSentFields() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        notificationService.markAsSent(testNotification);

        assertTrue(testNotification.isSent());
        assertNotNull(testNotification.getSentAt());
        assertEquals(testTime, testNotification.getSentAt());

        verify(notificationRepository, times(1)).save(testNotification);
    }

    @Test
    void markAsSent_AlreadySentNotification_ShouldUpdateSentAt() {
        testNotification.setSent(true);
        testNotification.setSentAt(testTime.minusHours(1));

        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        notificationService.markAsSent(testNotification);

        assertTrue(testNotification.isSent());
        assertEquals(testTime, testNotification.getSentAt()); // Should update sentAt

        verify(notificationRepository, times(1)).save(testNotification);
    }

    @Test
    void markAsSent_WithNullNotification_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            notificationService.markAsSent(null);
        });

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void schedule_VerifySentIsFalseByDefault() {
        notificationService.schedule(3L, "Test", "Message", NotificationType.MEAL_REMINDER, testTime);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                !notification.isSent()
        ));
    }

    @Test
    void schedule_VerifySentAtIsNullByDefault() {
        notificationService.schedule(3L, "Test", "Message", NotificationType.MEAL_REMINDER, testTime);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getSentAt() == null
        ));
    }

    @Test
    void markAsSent_VerifyRepositorySaveCalled() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        notificationService.markAsSent(testNotification);

        verify(notificationRepository, times(1)).save(testNotification);
    }

    @Test
    void schedule_MultipleNotifications_ShouldSaveAll() {
        for (int i = 1; i <= 5; i++) {
            notificationService.schedule(
                    (long) i,
                    "Title " + i,
                    "Message " + i,
                    NotificationType.MEAL_REMINDER,
                    testTime.plusMinutes(i)
            );
        }

        verify(notificationRepository, times(5)).save(any(Notification.class));
    }

    @Test
    void markAsSent_MultipleNotifications_ShouldUpdateAll() {
        Notification notification1 = Notification.builder().id(1L).build();
        Notification notification2 = Notification.builder().id(2L).build();
        Notification notification3 = Notification.builder().id(3L).build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        notificationService.markAsSent(notification1);
        notificationService.markAsSent(notification2);
        notificationService.markAsSent(notification3);

        verify(notificationRepository, times(1)).save(notification1);
        verify(notificationRepository, times(1)).save(notification2);
        verify(notificationRepository, times(1)).save(notification3);
    }

    @Test
    void schedule_WithLongTitleAndMessage_ShouldSave() {
        String longTitle = "A".repeat(1000);
        String longMessage = "B".repeat(2000);

        notificationService.schedule(3L, longTitle, longMessage, NotificationType.MEAL_REMINDER, testTime);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getTitle().equals(longTitle) &&
                        notification.getMessage().equals(longMessage)
        ));
    }

    @Test
    void markAsSent_VerifySentAtUsesCurrentTime() {
        Clock differentClock = Clock.fixed(testTime.plusHours(1).toInstant(ZoneOffset.of("+05:30")), ZoneId.of("Asia/Kolkata"));
        ReflectionTestUtils.setField(notificationService, "clock", differentClock);

        notificationService.markAsSent(testNotification);

        assertEquals(testTime.plusHours(1), testNotification.getSentAt());
    }

    @Test
    void schedule_WithDifferentUserIds_ShouldSaveCorrectly() {
        Long[] userIds = {1L, 2L, 3L, 100L, 999L};

        for (Long userId : userIds) {
            notificationService.schedule(userId, "Test", "Message", NotificationType.MEAL_REMINDER, testTime);
        }

        verify(notificationRepository, times(userIds.length)).save(argThat(notification ->
                java.util.Arrays.asList(userIds).contains(notification.getUserId())
        ));
    }
}
