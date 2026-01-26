package org.example.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class PushNotificationServiceImpl implements PushNotificationService {

    @Override
    public void sendBookingConfirmation(Long userId, LocalDate startDate, LocalDate endDate) {
        // Only send push notification, no DB entry
        System.out.println("Booking confirmation push notification sent to user " + userId +
                " for dates " + startDate + " to " + endDate);
        // TODO: Implement actual push notification logic here
    }

    @Override
    public void sendSingleMealBookingConfirmation(Long userId, LocalDate date) {
        // Only send push notification, no DB entry
        System.out.println("Single meal booking confirmation push notification sent to user " + userId +
                " for date " + date);
        // TODO: Implement actual push notification logic here
    }

    @Override
    public void sendCancellationConfirmation(Long userId, LocalDate date) {
        // Only send push notification, no DB entry
        System.out.println("Cancellation confirmation push notification sent to user " + userId +
                " for date " + date);
        // TODO: Implement actual push notification logic here
    }

    @Override
    public void sendMealReminder(Long userId, LocalDate date) {
        // Only send push notification, no DB entry
        System.out.println("Meal reminder push notification sent to user " + userId +
                " for date " + date);
        // TODO: Implement actual push notification logic here
    }

    @Override
    public void sendInactivityNudge(Long userId) {
        // Only send push notification, no DB entry
        System.out.println("Inactivity nudge push notification sent to user " + userId);
        // TODO: Implement actual push notification logic here
    }
}
