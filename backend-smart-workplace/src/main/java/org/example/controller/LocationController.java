package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.dto.LocationUpdateRequestDTO;
import org.example.service.UserLocationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
@Tag(name = "Location Management", description = "APIs for managing user location updates")
@SecurityRequirement(name = "bearerAuth")
public class LocationController {

    private final UserLocationService locationService;

    @PostMapping("/update")
    @Operation(
            summary = "Update user location",
            description = "Update the current location of a user. This endpoint tracks user coordinates for geofencing and location-based services."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Location updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"message\": \"Location updated successfully\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid location data provided",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"error\": \"Invalid latitude or longitude values\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User ID required",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"error\": \"User ID is required in X-USER-ID header\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"error\": \"User with specified ID not found\"}"
                            )
                    )
            )
    })
    public ResponseEntity<String> updateLocation(
            @Parameter(description = "Location update request containing latitude and longitude", required = true,
                    schema = @Schema(implementation = LocationUpdateRequestDTO.class))
            @RequestBody LocationUpdateRequestDTO request,
            @Parameter(description = "User ID from authentication header", required = true,
                    example = "12345")
            @RequestHeader("X-USER-ID") Long userId
    ) {
        locationService.saveLocation(userId, request);
        return ResponseEntity.ok("Location updated successfully");
    }
}
