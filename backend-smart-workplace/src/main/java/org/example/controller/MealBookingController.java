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
import jakarta.validation.Valid;
import org.example.dto.MealBookingRequestDTO;
import org.example.dto.MealCancelRequestDTO;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.example.service.MealBookingService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meals")
@Tag(name = "Meal Booking", description = "APIs for managing meal bookings including booking and cancellation")
@SecurityRequirement(name = "basicAuth")
public class MealBookingController {

    private final MealBookingService mealBookingService;
    private final UserRepository userRepository;

    public MealBookingController(
            MealBookingService mealBookingService,
            UserRepository userRepository
    ) {
        this.mealBookingService = mealBookingService;
        this.userRepository = userRepository;
    }

    @PostMapping("/book")
    @Operation(
            summary = "Book meals for a date range",
            description = "Book meals for one or more days within a specified date range. The system validates geofence, cutoff times, and prevents duplicate bookings."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Meals booked successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "Meals booked successfully")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid input or validation failed",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = "Meal already booked for 2026-01-25")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid credentials"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<?> bookMeals(
            @Parameter(
                    description = "Meal booking request details",
                    required = true,
                    schema = @Schema(implementation = MealBookingRequestDTO.class)
            )
            @Valid @RequestBody MealBookingRequestDTO request
    ) {
        // For testing without authentication, use a default user or create one if not exists
        String testEmail = "test@example.com";
        User user = userRepository.findByEmail(testEmail)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setName("Test User");
                    newUser.setEmail(testEmail);
                    newUser.setRole(Role.USER);
                    return userRepository.save(newUser);
                });

        mealBookingService.bookMeals(
                user,
                request.getStartDate(),
                request.getEndDate(),
                request.getLatitude(),
                request.getLongitude()
        );

        return ResponseEntity.ok("Meals booked successfully");
    }

    @DeleteMapping("/cancel")
    @Operation(
            summary = "Cancel meal booking",
            description = "Cancel an existing meal booking for a specific date. The user can only cancel their own bookings."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Meal booking cancelled successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "Meal booking cancelled successfully")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid date or no booking found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = "No booking found for this date")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid credentials"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<String> cancelMeal(
            @Parameter(
                    description = "Meal cancellation request",
                    required = true,
                    schema = @Schema(implementation = MealCancelRequestDTO.class)
            )
            @Valid @RequestBody MealCancelRequestDTO request
    ) {
        // For testing without authentication, use the same test user
        String testEmail = "test@example.com";
        User user =
                userRepository
                        .findByEmail(testEmail)
                        .orElseThrow(() -> new RuntimeException("Test user not found"));

        mealBookingService.cancelMeal(user, request.getDate());

        return ResponseEntity.ok("Meal booking cancelled successfully");
    }
}
