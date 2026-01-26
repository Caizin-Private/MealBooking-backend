package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.Notification;
import org.example.entity.NotificationType;
import org.example.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    public void schedule(
            Long userId,
            String title,
            String message,
            NotificationType type,
            LocalDateTime scheduleTime
    ) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .scheduledAt(scheduleTime)
                .sent(false)
                .build();

        notificationRepository.save(notification);
    }

    public void markAsSent(Notification notification) {
        if (notification == null) {
            throw new IllegalArgumentException("Notification cannot be null");
        }

        notification.setSent(true);
        notification.setSentAt(LocalDateTime.now(clock));
        notificationRepository.save(notification);
    }
}
