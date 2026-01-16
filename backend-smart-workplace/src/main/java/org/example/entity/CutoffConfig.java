package org.example.entity;

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
public class CutoffConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cutoff_time", nullable = false)
    private LocalTime cutoffTime;
}