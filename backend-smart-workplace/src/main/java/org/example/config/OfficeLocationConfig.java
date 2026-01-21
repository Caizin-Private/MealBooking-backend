package org.example.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "office")
@Getter
@Setter
public class OfficeLocationConfig {
    private double latitude;
    private double longitude;
    private double radiusMeters;
}
