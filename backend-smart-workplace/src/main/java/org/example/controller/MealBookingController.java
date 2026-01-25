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
import lombok.RequiredArgsConstructor;
import org.example.dto.SingleMealBookingRequestDTO;
import org.example.dto.SingleMealBookingResponseDTO;
import org.example.dto.RangeMealBookingRequestDTO;
import org.example.dto.RangeMealBookingResponseDTO;
import org.example.dto.UpcomingMealsRequestDTO;
import org.example.dto.UpcomingMealsResponseDTO;
import org.example.dto.CancelMealRequestDTO;
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
@Tag(name = "Meal Booking", description = "APIs for managing meal bookings")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class MealBookingController {

    private final MealBookingService mealBookingService;
    private final UserRepository userRepository;

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
                            schema = @Schema(implementation = SingleMealBookingResponseDTO.class),
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
    public ResponseEntity<SingleMealBookingResponseDTO> bookSingleMeal(
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

        SingleMealBookingResponseDTO response = mealBookingService.bookSingleMeal(user, request.getDate());

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

    @PostMapping("/upcoming")
    @Operation(
            summary = "Get user's upcoming meal bookings",
            description = "Retrieve all upcoming (BOOKED) meal bookings for a specific user. Shows dates of meals the user has booked but hasn't received yet."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Upcoming meal bookings retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpcomingMealsResponseDTO.class),
                            examples = @ExampleObject(value = "{\"bookedDates\": [\"2026-01-26\", \"2026-01-27\", \"2026-01-28\"]}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid user ID",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = "{\"error\": \"User not found\"}")
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
    public ResponseEntity<UpcomingMealsResponseDTO> getUpcomingMealBookings(
            @Parameter(
                    description = "Upcoming meals request",
                    required = true,
                    schema = @Schema(implementation = UpcomingMealsRequestDTO.class)
            )
            @Valid @RequestBody UpcomingMealsRequestDTO request
    ) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        UpcomingMealsResponseDTO response = mealBookingService.getUpcomingMeals(user);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/cancel")
    @Operation(
            summary = "Cancel meal booking by user ID and date",
            description = "Cancel a specific meal booking for a user on a given date. Validates date and booking existence."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Meal cancelled successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SingleMealBookingResponseDTO.class),
                            examples = @ExampleObject(value = "{\"success\": true, \"message\": \"Meal cancelled successfully for 2026-01-25\", \"bookingId\": 123, \"bookingDate\": \"2026-01-25\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid input or validation failed",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = "{\"success\": false, \"message\": \"Cannot cancel meals for past dates\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found - User or booking not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = "{\"success\": false, \"message\": \"No booking found for user 123 on 2026-01-25\"}")
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
    public ResponseEntity<SingleMealBookingResponseDTO> cancelMeal(
            @Parameter(
                    description = "Cancel meal request",
                    required = true,
                    schema = @Schema(implementation = CancelMealRequestDTO.class)
            )
            @Valid @RequestBody CancelMealRequestDTO request
    ) {
        SingleMealBookingResponseDTO response = mealBookingService.cancelMealByUserIdAndDate(request);

        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }
}