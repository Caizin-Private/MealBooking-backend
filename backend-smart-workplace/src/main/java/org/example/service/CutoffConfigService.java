package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.config.MealBookingConfig;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class CutoffConfigService {

    private final MealBookingConfig mealBookingConfig;

    public LocalTime getCurrentCutoffTime() {
        return mealBookingConfig.getCutoffTime();
    }

    public boolean isAfterCutoffTime() {
        return LocalTime.now().isAfter(getCurrentCutoffTime());
    }
}
