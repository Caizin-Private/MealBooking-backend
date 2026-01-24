package org.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;

@Configuration
@ConfigurationProperties(prefix = "meal-booking")
public class MealBookingConfig {

    private LocalTime cutoffTime = LocalTime.of(22, 0); // Default 10 PM

    public LocalTime getCutoffTime() {
        return cutoffTime;
    }

    public void setCutoffTime(LocalTime cutoffTime) {
        this.cutoffTime = cutoffTime;
    }
}
