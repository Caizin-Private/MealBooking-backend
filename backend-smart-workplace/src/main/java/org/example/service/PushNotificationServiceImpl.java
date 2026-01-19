package org.example.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class PushNotificationServiceImpl implements PushNotificationService {

    @Override
    public void sendBookingConfirmation(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        // TEMP: just log (later → email / push / SMS)
        System.out.println(
                "Meal booking confirmed for user " + userId +
                        " from " + startDate +
                        " to " + endDate
        );
    }

    @Override
    public void sendCancellationConfirmation(Long userId, LocalDate date) {
        // for now, keep it simple
        System.out.println(
                "Cancellation notification sent to user " + userId + " for date " + date
        );
    }


    @Override
    public void sendMealReminder(Long userId, LocalDate date) {
        // TODO: Implement reminder notification (email / push / SMS)
    }

    @Override
    public void sendMissedBookingNotification(Long userId, LocalDate date) {
        // TODO: implement later (email / push / SMS)
    }
}
