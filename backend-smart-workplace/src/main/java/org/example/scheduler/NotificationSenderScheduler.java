package org.example.scheduler;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.entity.Notification;
import org.example.repository.NotificationRepository;
import org.example.service.PushNotificationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "spring.task.scheduling",
        name = "enabled",
        havingValue = "true"
)
public class NotificationSenderScheduler {

    private final NotificationRepository notificationRepository;
    private final PushNotificationService pushNotificationService;
    private final Clock clock;

    @Scheduled(fixedDelay = 60_000) // every 1 minute
    @Transactional
    public void sendPendingNotifications() {

        LocalDateTime now = LocalDateTime.now(clock);

        List<Notification> pending =
                notificationRepository.findBySentFalseAndScheduledAtBefore(now);

        if (pending.isEmpty()) return;

        for (Notification notification : pending) {

            try {
                switch (notification.getType()) {

                    case MEAL_REMINDER ->
                            pushNotificationService.sendMealReminder(
                                    notification.getUserId(),
                                    notification.getScheduledAt().toLocalDate()
                            );

                    case MISSED_BOOKING ->
                            pushNotificationService.sendMissedBookingNotification(
                                    notification.getUserId(),
                                    notification.getScheduledAt().toLocalDate()
                            );

                    case INACTIVITY_NUDGE ->
                            pushNotificationService.sendInactivityNudge(
                                    notification.getUserId()
                            );

                    default -> {
                        System.out.println(
                                "[WARN] Unsupported notification type: "
                                        + notification.getType()
                        );
                        continue;
                    }
                }

                // ✅ mark as sent ONLY after success
                notification.setSent(true);
                notification.setSentAt(LocalDateTime.now(clock));
                notificationRepository.save(notification);

            } catch (Exception ex) {
                System.out.println(
                        "[ERROR] Notification send failed for id "
                                + notification.getId()
                );
                // ❗ keep sent=false → retry next run
            }
        }
    }
}
