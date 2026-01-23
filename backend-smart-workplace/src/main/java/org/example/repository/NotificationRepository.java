package org.example.repository;

import org.example.entity.Notification;
import org.example.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findBySentFalseAndScheduledAtBefore(
            LocalDateTime now
    );

    boolean existsByUserIdAndTypeAndScheduledAtBetween(
            Long userId,
            NotificationType type,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Notification> findByUserIdOrderBySentAtDesc(Long userId);

}