package org.example.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "cutoff_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Configuration entity for meal booking cutoff times")
public class CutoffConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier for the cutoff configuration", example = "1")
    private Long id;

    @Column(name = "cutoff_time", nullable = false)
    @Schema(description = "Cutoff time after which users cannot book meals for the current day", example = "22:00")
    private LocalTime cutoffTime;
}