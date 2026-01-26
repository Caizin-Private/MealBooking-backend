package org.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceImplTest {

    @InjectMocks
    private PushNotificationServiceImpl pushNotificationService;

    private Long testUserId;
    private LocalDate testDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() {
        testUserId = 3L;
        testDate = LocalDate.of(2026, 1, 26);
        startDate = LocalDate.of(2026, 1, 26);
        endDate = LocalDate.of(2026, 1, 30);

        // Set up to capture console output
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }

    @Test
    void sendSingleMealBookingConfirmation_ShouldSendNotification() {
        pushNotificationService.sendSingleMealBookingConfirmation(testUserId, testDate);

        String output = outputStream.toString();
        assertTrue(output.contains("Single meal booking confirmation push notification sent to user " + testUserId));
        assertTrue(output.contains("for date " + testDate));
    }

    @Test
    void sendSingleMealBookingConfirmation_WithNullUserId_ShouldStillSend() {
        pushNotificationService.sendSingleMealBookingConfirmation(null, testDate);

        String output = outputStream.toString();
        assertTrue(output.contains("Single meal booking confirmation push notification sent to user null"));
        assertTrue(output.contains("for date " + testDate));
    }

    @Test
    void sendSingleMealBookingConfirmation_WithNullDate_ShouldStillSend() {
        pushNotificationService.sendSingleMealBookingConfirmation(testUserId, null);

        String output = outputStream.toString();
        assertTrue(output.contains("Single meal booking confirmation push notification sent to user " + testUserId));
        assertTrue(output.contains("for date null"));
    }

    @Test
    void sendBookingConfirmation_ShouldSendNotification() {
        pushNotificationService.sendBookingConfirmation(testUserId, startDate, endDate);

        String output = outputStream.toString();
        assertTrue(output.contains("Booking confirmation push notification sent to user " + testUserId));
        assertTrue(output.contains("for dates " + startDate + " to " + endDate));
    }

    @Test
    void sendBookingConfirmation_SameStartAndEndDate_ShouldWork() {
        LocalDate sameDate = LocalDate.of(2026, 1, 26);
        pushNotificationService.sendBookingConfirmation(testUserId, sameDate, sameDate);

        String output = outputStream.toString();
        assertTrue(output.contains("Booking confirmation push notification sent to user " + testUserId));
        assertTrue(output.contains("for dates " + sameDate + " to " + sameDate));
    }

    @Test
    void sendBookingConfirmation_DifferentDateRanges_ShouldWork() {
        LocalDate start = LocalDate.of(2026, 1, 20);
        LocalDate end = LocalDate.of(2026, 1, 25);
        pushNotificationService.sendBookingConfirmation(testUserId, start, end);

        String output = outputStream.toString();
        assertTrue(output.contains("Booking confirmation push notification sent to user " + testUserId));
        assertTrue(output.contains("for dates " + start + " to " + end));
    }

    @Test
    void sendBookingConfirmation_WithNullDates_ShouldStillSend() {
        pushNotificationService.sendBookingConfirmation(testUserId, null, null);

        String output = outputStream.toString();
        assertTrue(output.contains("Booking confirmation push notification sent to user " + testUserId));
        assertTrue(output.contains("for dates null to null"));
    }

    @Test
    void sendCancellationConfirmation_ShouldSendNotification() {
        pushNotificationService.sendCancellationConfirmation(testUserId, testDate);

        String output = outputStream.toString();
        assertTrue(output.contains("Cancellation confirmation push notification sent to user " + testUserId));
        assertTrue(output.contains("for date " + testDate));
    }

    @Test
    void sendCancellationConfirmation_WithNullParameters_ShouldStillSend() {
        pushNotificationService.sendCancellationConfirmation(null, null);

        String output = outputStream.toString();
        assertTrue(output.contains("Cancellation confirmation push notification sent to user null"));
        assertTrue(output.contains("for date null"));
    }

    @Test
    void sendMealReminder_ShouldSendNotification() {
        pushNotificationService.sendMealReminder(testUserId, testDate);

        String output = outputStream.toString();
        assertTrue(output.contains("Meal reminder push notification sent to user " + testUserId));
        assertTrue(output.contains("for date " + testDate));
    }

    @Test
    void sendMealReminder_WithNullParameters_ShouldStillSend() {
        pushNotificationService.sendMealReminder(null, null);

        String output = outputStream.toString();
        assertTrue(output.contains("Meal reminder push notification sent to user null"));
        assertTrue(output.contains("for date null"));
    }

    @Test
    void sendInactivityNudge_ShouldSendNotification() {
        pushNotificationService.sendInactivityNudge(testUserId);

        String output = outputStream.toString();
        assertTrue(output.contains("Inactivity nudge push notification sent to user " + testUserId));
    }

    @Test
    void sendInactivityNudge_WithNullUserId_ShouldStillSend() {
        pushNotificationService.sendInactivityNudge(null);

        String output = outputStream.toString();
        assertTrue(output.contains("Inactivity nudge push notification sent to user null"));
    }

    @Test
    void multipleNotifications_ShouldSendAll() {
        pushNotificationService.sendSingleMealBookingConfirmation(testUserId, testDate);
        pushNotificationService.sendBookingConfirmation(testUserId, startDate, endDate);
        pushNotificationService.sendCancellationConfirmation(testUserId, testDate);
        pushNotificationService.sendMealReminder(testUserId, testDate);
        pushNotificationService.sendInactivityNudge(testUserId);

        String output = outputStream.toString();

        // Verify all notifications were sent
        assertTrue(output.contains("Single meal booking confirmation push notification sent to user " + testUserId));
        assertTrue(output.contains("Booking confirmation push notification sent to user " + testUserId));
        assertTrue(output.contains("Cancellation confirmation push notification sent to user " + testUserId));
        assertTrue(output.contains("Meal reminder push notification sent to user " + testUserId));
        assertTrue(output.contains("Inactivity nudge push notification sent to user " + testUserId));

        // Count occurrences - should be 5 notifications
        int notificationCount = 0;
        String[] lines = output.split(System.lineSeparator());
        for (String line : lines) {
            if (line.contains("push notification sent to user")) {
                notificationCount++;
            }
        }
        assertEquals(5, notificationCount);
    }
}
