package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.Notification;
import org.example.entity.NotificationType;
import org.example.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PushNotificationServiceImpl implements PushNotificationService {

    private final NotificationRepository notificationRepository;

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
        System.out.println(
                "[PUSH] Meal reminder sent to user " + userId + " for " + date
        );
    }

    @Override
    public void sendMissedBookingNotification(Long userId, LocalDate date) {
        System.out.println(
                "[MISSED BOOKING] User " + userId + " missed booking for " + date
        );
    }

    @Override
    public void sendInactivityNudge(Long userId) {
        System.out.println(
                "[INACTIVITY NUDGE] User " + userId + " has been inactive"
        );
    }

    public void sendLunchDefaultedNotification(Long userId, LocalDate date) {
        System.out.println(
                "[LUNCH DEFAULTED] User " + userId + " defaulted on " + date
        );
    }



}
