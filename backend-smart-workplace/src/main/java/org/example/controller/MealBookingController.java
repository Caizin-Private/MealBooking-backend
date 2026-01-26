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
import org.example.entity.MealBooking;
import org.example.entity.User;
import org.example.service.AzureOAuth2UserService;
import org.example.service.MealBookingService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meals")
@Tag(name = "Meal Booking", description = "APIs for managing meal bookings including booking and cancellation")
@SecurityRequirement(name = "bearerAuth")
public class MealBookingController {

        private final MealBookingService mealBookingService;
        private final AzureOAuth2UserService azureOAuth2UserService;

        public MealBookingController(
                        MealBookingService mealBookingService,
                        AzureOAuth2UserService azureOAuth2UserService) {
                this.mealBookingService = mealBookingService;
                this.azureOAuth2UserService = azureOAuth2UserService;
        }


        @GetMapping("/test")
        @Operation(summary = "Test endpoint", description = "Simple test endpoint to verify controller is working")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Test successful - Controller is working", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = String.class), examples = @ExampleObject(value = "Controller is working"))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid credentials"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<String> testEndpoint() {
                return ResponseEntity.ok("Controller is working");
        }


        @PostMapping("/book")
        @Operation(summary = "Book meals for a date range", description = "Book meals for one or more days within a specified date range. The system validates geofence, cutoff times, and prevents duplicate bookings.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Meals booked successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = String.class), examples = @ExampleObject(value = "Meals booked successfully"))),
                        @ApiResponse(responseCode = "400", description = "Bad request - Invalid input or validation failed", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = "Meal already booked for 2026-01-25"))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid credentials"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<?> bookMeals(
                        @Parameter(description = "Meal booking request details", required = true, schema = @Schema(implementation = MealBookingRequestDTO.class)) @Valid @RequestBody MealBookingRequestDTO request) {

                User user = azureOAuth2UserService.getOrCreateAuthenticatedUser();

                mealBookingService.bookMeals(
                                user,
                                request.getStartDate(),
                                request.getEndDate(),
                                request.getLatitude(),
                                request.getLongitude());

                return ResponseEntity.ok(java.util.Collections.singletonMap("message", "Meals booked successfully"));
        }

        @DeleteMapping("/cancel")
        @Operation(summary = "Cancel meal booking", description = "Cancel an existing meal booking for a specific date. The user can only cancel their own bookings.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Meal booking cancelled successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = String.class), examples = @ExampleObject(value = "Meal booking cancelled successfully"))),
                        @ApiResponse(responseCode = "400", description = "Bad request - Invalid date or no booking found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = "No booking found for this date"))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid credentials"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<java.util.Map<String, String>> cancelMeal(
                        @Parameter(description = "Meal cancellation request", required = true, schema = @Schema(implementation = MealCancelRequestDTO.class)) @Valid @RequestBody MealCancelRequestDTO request) {

                User user = azureOAuth2UserService.getOrCreateAuthenticatedUser();

                mealBookingService.cancelMeal(user, request.getDate());

                return ResponseEntity.ok(
                                java.util.Collections.singletonMap("message", "Meal booking cancelled successfully"));
        }

        @GetMapping("/my-bookings")
        @Operation(summary = "Get my bookings", description = "Retrieve all meal bookings for the authenticated user")
        public ResponseEntity<List<MealBooking>> getMyBookings() {
                User user = azureOAuth2UserService.getOrCreateAuthenticatedUser();
                return ResponseEntity.ok(mealBookingService.getBookingsByUser(user));
        }
}
