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
        Notification notification = Notification.builder()
                .userId(userId)
                .title("Meal Booking Confirmed")
                .message("Your meals have been booked from " + startDate + " to " + endDate)
                .type(NotificationType.BOOKING_CONFIRMATION)
                .sent(false)
                .scheduledAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        System.out.println("Range booking notification stored for user " + userId);
    }

    @Override
    public void sendSingleMealBookingConfirmation(Long userId, LocalDate date) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title("Meal Booking Confirmed")
                .message("Your meal has been booked for " + date)
                .type(NotificationType.BOOKING_CONFIRMATION)
                .sent(false)
                .scheduledAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        System.out.println("Single meal booking notification stored for user " + userId);
    }

    @Override
    public void sendCancellationConfirmation(Long userId, LocalDate date) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title("Booking Cancelled")
                .message("Your meal booking for " + date + " has been cancelled")
                .type(NotificationType.CANCELLATION_CONFIRMATION)
                .sent(false)
                .scheduledAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        System.out.println("Cancellation notification stored for user " + userId);
    }

    @Override
    public void sendMealReminder(Long userId, LocalDate date) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title("Meal Reminder")
                .message("Don't forget! You have a meal booked for " + date)
                .type(NotificationType.MEAL_REMINDER)
                .sent(false)
                .scheduledAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        System.out.println("Meal reminder notification stored for user " + userId);
    }

    @Override
    public void sendMissedBookingNotification(Long userId, LocalDate date) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title("Missed Booking")
                .message("You missed your meal booking for " + date)
                .type(NotificationType.MISSED_BOOKING)
                .sent(false)
                .scheduledAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        System.out.println("Missed booking notification stored for user " + userId);
    }

    @Override
    public void sendInactivityNudge(Long userId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title("Book Your Meals")
                .message("Don't forget to book your meals for the upcoming days!")
                .type(NotificationType.INACTIVITY_NUDGE)
                .sent(false)
                .scheduledAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        System.out.println("Inactivity nudge notification stored for user " + userId);
    }
}
