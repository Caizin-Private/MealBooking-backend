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

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private PushNotificationServiceImpl pushNotificationService;

    private Long testUserId;
    private LocalDate testDate;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        testUserId = 3L;
        testDate = LocalDate.of(2026, 1, 26);
        startDate = LocalDate.of(2026, 1, 26);
        endDate = LocalDate.of(2026, 1, 30);
    }

    @Test
    void sendSingleMealBookingConfirmation_ShouldCreateNotification() {
        pushNotificationService.sendSingleMealBookingConfirmation(testUserId, testDate);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getUserId().equals(testUserId) &&
                        notification.getTitle().equals("Meal Booking Confirmed") &&
                        notification.getMessage().equals("Your meal has been booked for " + testDate) &&
                        notification.getType().equals(NotificationType.BOOKING_CONFIRMATION) &&
                        !notification.isSent() &&
                        notification.getScheduledAt() != null
        ));
    }

    @Test
    void sendBookingConfirmation_ShouldCreateRangeNotification() {
        pushNotificationService.sendBookingConfirmation(testUserId, startDate, endDate);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getUserId().equals(testUserId) &&
                        notification.getTitle().equals("Meal Booking Confirmed") &&
                        notification.getMessage().equals("Your meals have been booked from " + startDate + " to " + endDate) &&
                        notification.getType().equals(NotificationType.BOOKING_CONFIRMATION) &&
                        !notification.isSent() &&
                        notification.getScheduledAt() != null
        ));
    }

    @Test
    void sendCancellationConfirmation_ShouldCreateCancellationNotification() {
        pushNotificationService.sendCancellationConfirmation(testUserId, testDate);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getUserId().equals(testUserId) &&
                        notification.getTitle().equals("Booking Cancelled") &&
                        notification.getMessage().equals("Your meal booking for " + testDate + " has been cancelled") &&
                        notification.getType().equals(NotificationType.CANCELLATION_CONFIRMATION) &&
                        !notification.isSent() &&
                        notification.getScheduledAt() != null
        ));
    }

    @Test
    void sendMealReminder_ShouldCreateReminderNotification() {
        pushNotificationService.sendMealReminder(testUserId, testDate);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getUserId().equals(testUserId) &&
                        notification.getTitle().equals("Meal Reminder") &&
                        notification.getMessage().equals("Don't forget! You have a meal booked for " + testDate) &&
                        notification.getType().equals(NotificationType.MEAL_REMINDER) &&
                        !notification.isSent() &&
                        notification.getScheduledAt() != null
        ));
    }

    @Test
    void sendMissedBookingNotification_ShouldCreateMissedBookingNotification() {
        pushNotificationService.sendMissedBookingNotification(testUserId, testDate);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getUserId().equals(testUserId) &&
                        notification.getTitle().equals("Missed Booking") &&
                        notification.getMessage().equals("You missed your meal booking for " + testDate) &&
                        notification.getType().equals(NotificationType.MISSED_BOOKING) &&
                        !notification.isSent() &&
                        notification.getScheduledAt() != null
        ));
    }

    @Test
    void sendInactivityNudge_ShouldCreateInactivityNotification() {
        pushNotificationService.sendInactivityNudge(testUserId);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getUserId().equals(testUserId) &&
                        notification.getTitle().equals("Book Your Meals") &&
                        notification.getMessage().equals("Don't forget to book your meals for the upcoming days!") &&
                        notification.getType().equals(NotificationType.INACTIVITY_NUDGE) &&
                        !notification.isSent() &&
                        notification.getScheduledAt() != null
        ));
    }

    @Test
    void sendSingleMealBookingConfirmation_WithNullUserId_ShouldStillSave() {
        pushNotificationService.sendSingleMealBookingConfirmation(null, testDate);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getUserId() == null &&
                        notification.getTitle().equals("Meal Booking Confirmed") &&
                        notification.getType().equals(NotificationType.BOOKING_CONFIRMATION)
        ));
    }

    @Test
    void sendSingleMealBookingConfirmation_WithNullDate_ShouldStillSave() {
        pushNotificationService.sendSingleMealBookingConfirmation(testUserId, null);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getUserId().equals(testUserId) &&
                        notification.getTitle().equals("Meal Booking Confirmed") &&
                        notification.getType().equals(NotificationType.BOOKING_CONFIRMATION)
        ));
    }

    @Test
    void sendBookingConfirmation_WithNullDates_ShouldStillSave() {
        pushNotificationService.sendBookingConfirmation(testUserId, null, null);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getUserId().equals(testUserId) &&
                        notification.getTitle().equals("Meal Booking Confirmed") &&
                        notification.getType().equals(NotificationType.BOOKING_CONFIRMATION)
        ));
    }

    @Test
    void sendBookingConfirmation_SameStartAndEndDate_ShouldWork() {
        LocalDate sameDate = LocalDate.of(2026, 1, 26);
        pushNotificationService.sendBookingConfirmation(testUserId, sameDate, sameDate);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getMessage().equals("Your meals have been booked from " + sameDate + " to " + sameDate)
        ));
    }

    @Test
    void sendCancellationConfirmation_WithNullParameters_ShouldStillSave() {
        pushNotificationService.sendCancellationConfirmation(null, null);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getUserId() == null &&
                        notification.getTitle().equals("Booking Cancelled") &&
                        notification.getType().equals(NotificationType.CANCELLATION_CONFIRMATION)
        ));
    }

    @Test
    void sendMealReminder_WithNullParameters_ShouldStillSave() {
        pushNotificationService.sendMealReminder(null, null);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getUserId() == null &&
                        notification.getTitle().equals("Meal Reminder") &&
                        notification.getType().equals(NotificationType.MEAL_REMINDER)
        ));
    }

    @Test
    void sendMissedBookingNotification_WithNullParameters_ShouldStillSave() {
        pushNotificationService.sendMissedBookingNotification(null, null);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getUserId() == null &&
                        notification.getTitle().equals("Missed Booking") &&
                        notification.getType().equals(NotificationType.MISSED_BOOKING)
        ));
    }

    @Test
    void sendInactivityNudge_WithNullUserId_ShouldStillSave() {
        pushNotificationService.sendInactivityNudge(null);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getUserId() == null &&
                        notification.getTitle().equals("Book Your Meals") &&
                        notification.getType().equals(NotificationType.INACTIVITY_NUDGE)
        ));
    }

    @Test
    void sendSingleMealBookingNotification_VerifySentIsFalse() {
        pushNotificationService.sendSingleMealBookingConfirmation(testUserId, testDate);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                !notification.isSent()
        ));
    }

    @Test
    void sendBookingConfirmation_VerifySentIsFalse() {
        pushNotificationService.sendBookingConfirmation(testUserId, startDate, endDate);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                !notification.isSent()
        ));
    }

    @Test
    void sendCancellationConfirmation_VerifySentIsFalse() {
        pushNotificationService.sendCancellationConfirmation(testUserId, testDate);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                !notification.isSent()
        ));
    }

    @Test
    void sendMealReminder_VerifySentIsFalse() {
        pushNotificationService.sendMealReminder(testUserId, testDate);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                !notification.isSent()
        ));
    }

    @Test
    void sendMissedBookingNotification_VerifySentIsFalse() {
        pushNotificationService.sendMissedBookingNotification(testUserId, testDate);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                !notification.isSent()
        ));
    }

    @Test
    void sendInactivityNudge_VerifySentIsFalse() {
        pushNotificationService.sendInactivityNudge(testUserId);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                !notification.isSent()
        ));
    }

    @Test
    void sendSingleMealBookingConfirmation_VerifyScheduledAtIsSet() {
        pushNotificationService.sendSingleMealBookingConfirmation(testUserId, testDate);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getScheduledAt() != null
        ));
    }

    @Test
    void sendBookingConfirmation_VerifyScheduledAtIsSet() {
        pushNotificationService.sendBookingConfirmation(testUserId, startDate, endDate);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getScheduledAt() != null
        ));
    }

    @Test
    void sendCancellationConfirmation_VerifyScheduledAtIsSet() {
        pushNotificationService.sendCancellationConfirmation(testUserId, testDate);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getScheduledAt() != null
        ));
    }

    @Test
    void sendMealReminder_VerifyScheduledAtIsSet() {
        pushNotificationService.sendMealReminder(testUserId, testDate);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getScheduledAt() != null
        ));
    }

    @Test
    void sendMissedBookingNotification_VerifyScheduledAtIsSet() {
        pushNotificationService.sendMissedBookingNotification(testUserId, testDate);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getScheduledAt() != null
        ));
    }

    @Test
    void sendInactivityNudge_VerifyScheduledAtIsSet() {
        pushNotificationService.sendInactivityNudge(testUserId);

        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getScheduledAt() != null
        ));
    }

    @Test
    void sendSingleMealBookingConfirmation_VerifyRepositorySaveCalled() {
        pushNotificationService.sendSingleMealBookingConfirmation(testUserId, testDate);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void sendBookingConfirmation_VerifyRepositorySaveCalled() {
        pushNotificationService.sendBookingConfirmation(testUserId, startDate, endDate);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void sendCancellationConfirmation_VerifyRepositorySaveCalled() {
        pushNotificationService.sendCancellationConfirmation(testUserId, testDate);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void sendMealReminder_VerifyRepositorySaveCalled() {
        pushNotificationService.sendMealReminder(testUserId, testDate);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void sendMissedBookingNotification_VerifyRepositorySaveCalled() {
        pushNotificationService.sendMissedBookingNotification(testUserId, testDate);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void sendInactivityNudge_VerifyRepositorySaveCalled() {
        pushNotificationService.sendInactivityNudge(testUserId);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void multipleNotifications_ShouldSaveAll() {
        pushNotificationService.sendSingleMealBookingConfirmation(testUserId, testDate);
        pushNotificationService.sendMealReminder(testUserId, testDate);
        pushNotificationService.sendInactivityNudge(testUserId);

        verify(notificationRepository, times(3)).save(any(Notification.class));
    }

    @Test
    void sendBookingConfirmation_DifferentDateRanges_ShouldWork() {
        LocalDate start1 = LocalDate.of(2026, 1, 1);
        LocalDate end1 = LocalDate.of(2026, 1, 5);
        LocalDate start2 = LocalDate.of(2026, 2, 1);
        LocalDate end2 = LocalDate.of(2026, 2, 10);

        pushNotificationService.sendBookingConfirmation(testUserId, start1, end1);
        pushNotificationService.sendBookingConfirmation(testUserId, start2, end2);

        verify(notificationRepository, times(2)).save(argThat(notification ->
                notification.getType().equals(NotificationType.BOOKING_CONFIRMATION)
        ));
    }
}
