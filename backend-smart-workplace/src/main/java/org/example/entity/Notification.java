package org.example.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Notification entity representing a notification message for a user")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier for the notification", example = "1")
    private Long id;

    @Column(name = "user_id", nullable = false)
    @Schema(description = "ID of the user who will receive the notification", example = "3")
    private Long userId;

    @Column(nullable = false)
    @Schema(description = "Title of the notification message", example = "Lunch Available")
    private String title;

    @Column(nullable = false, length = 500)
    @Schema(description = "Detailed message content of the notification", example = "Great! You're within office range and confirmed for lunch today. Your meal booking is active.")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "Type of the notification", allowableValues = {"BOOKING_CONFIRMATION", "CANCELLATION_CONFIRMATION", "MEAL_REMINDER", "MISSED_BOOKING", "INACTIVITY_NUDGE"})
    private NotificationType type;

    @Column(nullable = false)
    @Schema(description = "Flag indicating if the notification has been sent to the user's device", example = "false")
    private boolean sent;

    @Column(name = "scheduled_at", nullable = false)
    @Schema(description = "Timestamp when the notification was scheduled/created", example = "2026-01-25T15:30:00")
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    @Schema(description = "Timestamp when the notification was actually sent to the user's device", example = "2026-01-25T15:31:00")
    private LocalDateTime sentAt;
}
