package org.example.service;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
public class PushNotificationServiceImpl implements PushNotificationService {

    @Override
    public void sendBookingConfirmation(Long userId, LocalDate startDate, LocalDate endDate) {
        System.out.println("Booking confirmation push notification sent to user " + userId +
                " for dates " + startDate + " to " + endDate);
    }

    @Override
    public void sendSingleMealBookingConfirmation(Long userId, LocalDate date) {
        System.out.println("Single meal booking confirmation push notification sent to user " + userId +
                " for date " + date);
    }

    @Override
    public void sendCancellationConfirmation(Long userId, LocalDate date) {
        System.out.println("Cancellation confirmation push notification sent to user " + userId +
                " for date " + date);
    }

    @Override
    public void sendMealReminder(Long userId, LocalDate date) {
        System.out.println("Meal reminder push notification sent to user " + userId +
                " for date " + date);
    }

    @Override
    public void sendInactivityNudge(Long userId) {
        System.out.println("Inactivity nudge push notification sent to user " + userId);
    }
}
