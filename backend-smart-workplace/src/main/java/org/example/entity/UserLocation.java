package org.example.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_location")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "UserLocation entity representing the current location of a user for geofence tracking")
public class UserLocation {

    @Id
    @Schema(description = "Unique identifier for the user (same as user ID)", example = "3")
    private Long userId;

    @Column(nullable = false)
    @Schema(description = "Latitude coordinate of the user's current location", example = "18.5204")
    private Double latitude;

    @Column(nullable = false)
    @Schema(description = "Longitude coordinate of the user's current location", example = "73.8567")
    private Double longitude;

    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Timestamp when the location was last updated", example = "2026-01-25T15:30:00")
    private LocalDateTime updatedAt;
}
