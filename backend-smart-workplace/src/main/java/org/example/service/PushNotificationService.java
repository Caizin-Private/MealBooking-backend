package org.example.service;

import java.time.LocalDate;

public interface PushNotificationService {

    void sendSingleMealBookingConfirmation(Long userId, LocalDate date);

    void sendBookingConfirmation(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );
    void sendCancellationConfirmation(Long userId, LocalDate date);

    void sendMealReminder(Long userId, LocalDate date);

    void sendMissedBookingNotification(Long userId, LocalDate date);

    void sendInactivityNudge(Long userId);

    void sendLunchDefaultedNotification(Long userId, LocalDate date);


}
