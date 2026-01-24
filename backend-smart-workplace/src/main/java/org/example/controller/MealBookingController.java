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
import org.example.dto.*;
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
@SecurityRequirement(name = "bearerAuth")
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
        // create user for testing
        String testEmail = "test@example.com";
        User user = userRepository.findByEmail(testEmail)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setName("Test User");
                    newUser.setEmail(testEmail);
                    newUser.setRole(Role.USER);
                    newUser.setCreatedAt(java.time.LocalDateTime.now());
                    newUser.setExternalId("test-external-id");
                    newUser.setLastLoginAt(java.time.LocalDateTime.now());
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




    @PostMapping("/book-single")
    @Operation(
            summary = "Book meal for a single day",
            description = "Book a meal for a specific date. Validates date, cutoff times, and prevents duplicate bookings."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Meal booked successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MealBookingResponseDTO.class),
                            examples = @ExampleObject(value = "{\"success\": true, \"message\": \"Meal booked successfully for 2026-01-25\", \"bookingId\": 123, \"bookingDate\": \"2026-01-25\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid input or validation failed",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = "{\"success\": false, \"message\": \"Meal already booked for 2026-01-25\"}")
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
    public ResponseEntity<MealBookingResponseDTO> bookSingleMeal(
            @Parameter(
                    description = "Single day meal booking request",
                    required = true,
                    schema = @Schema(implementation = SingleMealBookingRequestDTO.class)
            )
            @Valid @RequestBody SingleMealBookingRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        MealBookingResponseDTO response = mealBookingService.bookSingleMeal(user, request.getDate());

        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }


    @PostMapping("/book-range")
    @Operation(
            summary = "Book meals for a date range",
            description = "Book meals for multiple dates. Validates each date, cutoff times, and prevents duplicate bookings."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Range booking processed successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RangeMealBookingResponseDTO.class),
                            examples = @ExampleObject(value = "{\"success\": true, \"message\": \"All meals booked successfully from 2026-01-25 to 2026-01-27\", \"bookedDates\": [\"2026-01-25\", \"2026-01-26\", \"2026-01-27\"]}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid input or validation failed",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = "{\"success\": false, \"message\": \"Some bookings failed\", \"failedBookings\": [\"2026-01-26 - Already booked\"]}")
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
    public ResponseEntity<RangeMealBookingResponseDTO> bookRangeMeals(
            @Parameter(
                    description = "Range meal booking request",
                    required = true,
                    schema = @Schema(implementation = RangeMealBookingRequestDTO.class)
            )
            @Valid @RequestBody RangeMealBookingRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        RangeMealBookingResponseDTO response = mealBookingService.bookRangeMeals(
                user,
                request.getStartDate(),
                request.getEndDate()
        );

        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }
}
