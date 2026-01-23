package org.example.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "meal_bookings",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "booking_date"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Meal booking entity representing a user's meal reservation")
public class MealBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier for the meal booking", example = "1")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @Schema(description = "User who made the booking")
    private User user;

    @Column(name = "booking_date", nullable = false)
    @Schema(description = "Date of the meal booking", example = "2026-01-25")
    private LocalDate bookingDate;

    @Column(name = "booked_at", nullable = false)
    @Schema(description = "Timestamp when the booking was made", example = "2026-01-22T10:30:00")
    private LocalDateTime bookedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "Status of the meal booking", allowableValues = {"BOOKED", "DEFAULT", "CANCELLED", "MISSED"})
    private BookingStatus status;

    @Column(name = "available_for_lunch")
    @Builder.Default
    @Schema(description = "Whether the meal is available for lunch", example = "true")
    private Boolean availableForLunch = true;
}
