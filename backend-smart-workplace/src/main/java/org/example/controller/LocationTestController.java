package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.LocationUpdateRequestDTO;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.example.service.UserLocationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test/location")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Location Testing", description = "Testing APIs for user location with time override")
public class LocationTestController {

    private final UserLocationService locationService;
    private final UserRepository userRepository;

    @PostMapping("/update-with-time")
    @Operation(
            summary = "Update user location with custom time (for testing)",
            description = "Test location updates with a specific timestamp to simulate different times of day"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Location updated successfully with test time",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "Location updated successfully with test time"
                            )
                    )
            )
    })
    public ResponseEntity<String> updateLocationWithTime(
            @Valid @RequestBody LocationUpdateRequestDTO request,
            @Parameter(description = "User ID", required = true)
            @RequestParam Long userId,
            @Parameter(description = "Test time in ISO format (e.g., '2026-01-27T13:30:00Z')", required = true)
            @RequestParam String testTime
    ) {
        // Create a custom clock with the test time
        Instant testInstant = Instant.parse(testTime);
        ZoneId zone = ZoneId.systemDefault();
        Clock testClock = Clock.fixed(testInstant, zone);

        // Create a test service instance with the custom clock
        UserLocationService testService = new UserLocationService(
                locationService.repository,
                locationService.mealBookingRepository,
                locationService.userRepository,
                testClock
        );

        testService.saveLocation(userId, request);

        ZonedDateTime testDateTime = ZonedDateTime.ofInstant(testInstant, zone);
        return ResponseEntity.ok("Location updated successfully with test time: " + testDateTime);
    }

    @PostMapping("/test-five-times")
    @Operation(
            summary = "Test user location 5 times with different scenarios",
            description = "Simulate 5 location updates at different times to test geofencing logic"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Location testing completed",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Location testing completed\",\"results\":[...]}"
                            )
                    )
            )
    })
    public ResponseEntity<Map<String, Object>> testLocationFiveTimes(
            @Parameter(description = "User ID", required = true)
            @RequestParam Long userId,
            @Parameter(description = "Base date for testing (YYYY-MM-DD)", required = false)
            @RequestParam(required = false) String baseDate
    ) {
        LocalDate testDate = baseDate != null ? LocalDate.parse(baseDate) : LocalDate.now();

        // Verify user exists or create test user
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            // For testing, create a simple test user response
            Map<String, Object> testResponse = new HashMap<>();
            testResponse.put("message", "Test mode - User not found, showing test scenario");
            testResponse.put("userId", userId);
            testResponse.put("testDate", testDate);
            testResponse.put("note", "Create a user in database first, or this shows what the test would do");

            List<Map<String, Object>> testResults = new ArrayList<>();
            testResults.add(Map.of(
                    "scenario", "Test would run 6 location checks",
                    "times", "12:00, 12:30, 13:00, 13:30, 14:00, 14:30",
                    "locations", "Office (18.5204, 73.8567) and Outside (19.0760, 72.8777)",
                    "expected", "Geofence logic would be tested"
            ));

            testResponse.put("testResults", testResults);
            return ResponseEntity.ok(testResponse);
        }

        List<Map<String, Object>> results = new ArrayList<>();

        // Test scenarios: Different times and locations
        List<LocationTestScenario> scenarios = List.of(
                // 1. Start of lunch time (12:00) - inside geofence
                new LocationTestScenario("12:00", 18.5204, 73.8567, "Lunch start - inside geofence"),

                // 2. Early lunch (12:30) - inside geofence
                new LocationTestScenario("12:30", 18.5204, 73.8567, "Early lunch - inside geofence"),

                // 3. Mid lunch (13:00) - outside geofence
                new LocationTestScenario("13:00", 19.0760, 72.8777, "Mid lunch - outside geofence"),

                // 4. Late lunch (13:30) - back inside geofence
                new LocationTestScenario("13:30", 18.5204, 73.8567, "Late lunch - back inside geofence"),

                // 5. Near end (14:00) - outside geofence
                new LocationTestScenario("14:00", 19.0760, 72.8777, "Near end - outside geofence"),

                // 6. End of lunch time (14:30) - outside geofence (should default if not available)
                new LocationTestScenario("14:30", 19.0760, 72.8777, "End of lunch - outside geofence")
        );

        for (int i = 0; i < scenarios.size(); i++) {
            LocationTestScenario scenario = scenarios.get(i);

            try {
                // Create test time
                String testDateTime = testDate + "T" + scenario.time + ":00Z";
                Instant testInstant = Instant.parse(testDateTime);
                ZoneId zone = ZoneId.systemDefault();
                Clock testClock = Clock.fixed(testInstant, zone);

                // Create test service
                UserLocationService testService = new UserLocationService(
                        locationService.repository,
                        locationService.mealBookingRepository,
                        locationService.userRepository,
                        testClock
                );

                // Create location request
                LocationUpdateRequestDTO request = LocationUpdateRequestDTO.builder()
                        .latitude(scenario.latitude)
                        .longitude(scenario.longitude)
                        .build();

                // Execute location update
                testService.saveLocation(userId, request);

                // Record result
                Map<String, Object> result = new HashMap<>();
                result.put("testNumber", i + 1);
                result.put("time", scenario.time);
                result.put("description", scenario.description);
                result.put("latitude", scenario.latitude);
                result.put("longitude", scenario.longitude);
                result.put("status", "SUCCESS");
                result.put("testDateTime", testDateTime);

                results.add(result);

                log.info("Location test {} completed for user {} at {}", i + 1, userId, scenario.time);

            } catch (Exception e) {
                Map<String, Object> result = new HashMap<>();
                result.put("testNumber", i + 1);
                result.put("time", scenario.time);
                result.put("description", scenario.description);
                result.put("latitude", scenario.latitude);
                result.put("longitude", scenario.longitude);
                result.put("status", "FAILED");
                result.put("error", e.getMessage());

                results.add(result);

                log.error("Location test {} failed for user {}: {}", i + 1, userId, e.getMessage());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Location testing completed");
        response.put("userId", userId);
        response.put("testDate", testDate);
        response.put("totalTests", scenarios.size());
        response.put("results", results);

        return ResponseEntity.ok(response);
    }

    // Helper class for test scenarios
    private static class LocationTestScenario {
        String time;
        double latitude;
        double longitude;
        String description;

        LocationTestScenario(String time, double latitude, double longitude, String description) {
            this.time = time;
            this.latitude = latitude;
            this.longitude = longitude;
            this.description = description;
        }
    }
}
