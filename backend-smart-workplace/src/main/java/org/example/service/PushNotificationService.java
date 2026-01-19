package org.example.service;

import java.time.LocalDate;

public interface PushNotificationService {

    void sendBookingConfirmation(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );
    void sendCancellationConfirmation(Long userId, LocalDate date);
}
