package org.example.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.entity.Notification;
import org.example.repository.NotificationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationSenderScheduler {

    private final NotificationRepository notificationRepository;

    @Scheduled(fixedDelay = 60000) // every 1 min
    public void sendPendingNotifications() {

        List<Notification> notifications =
                notificationRepository
                        .findBySentFalseAndScheduledAtBefore(
                                LocalDateTime.now()
                        );

        for (Notification n : notifications) {

            // TEMP: simulate sending
            System.out.println(
                    "Sending notification to user " +
                            n.getUserId() + ": " + n.getMessage()
            );

            n.setSent(true);
            n.setSentAt(LocalDateTime.now());
        }

        notificationRepository.saveAll(notifications);
    }
}
