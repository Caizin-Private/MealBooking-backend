package org.example.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class CutoffConfigRequest {

    @NotNull
    private LocalTime cutoffTime;
}
