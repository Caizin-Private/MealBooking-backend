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
import org.example.dto.LocationUpdateRequestDTO;
import org.example.service.UserLocationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@RestController
@RequestMapping("/api/test/location")
@RequiredArgsConstructor
@Tag(name = "Location Testing", description = "Testing APIs for user location with time override")
public class LocationTestController {

    private final UserLocationService locationService;

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
}
