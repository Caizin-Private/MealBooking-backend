package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.MealBookingRequestDTO;
import org.example.dto.MealCancelRequestDTO;
import org.example.entity.User;
import org.example.service.AuthenticatedUserService;
import org.example.service.MealBookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meals")
public class MealBookingController {

    private final MealBookingService mealBookingService;
    private final AuthenticatedUserService authenticatedUserService;

    public MealBookingController(
            MealBookingService mealBookingService,
            AuthenticatedUserService authenticatedUserService
    ) {
        this.mealBookingService = mealBookingService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping("/book")
    public ResponseEntity<?> bookMeals(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MealBookingRequestDTO request
    ) {
        User user = authenticatedUserService.loadOrCreateUser(jwt);

        mealBookingService.bookMeals(
                user,
                request.getStartDate(),
                request.getEndDate(),
                request.getLatitude(),
                request.getLongitude()
        );

        return ResponseEntity.ok("Meals booked successfully");
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @DeleteMapping("/cancel")
    public ResponseEntity<String> cancelMeal(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MealCancelRequestDTO request
    ) {
        User user = authenticatedUserService.loadOrCreateUser(jwt);

        mealBookingService.cancelMeal(user, request.getDate());

        return ResponseEntity.ok("Meal booking cancelled successfully");
    }
}
