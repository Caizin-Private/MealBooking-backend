package org.example.controller;

import org.example.dto.MealBookingRequestDTO;
import org.example.entity.User;
import org.example.service.MealBookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/meals")
public class MealBookingController {

    private final MealBookingService mealBookingService;

    public MealBookingController(MealBookingService mealBookingService) {
        this.mealBookingService = mealBookingService;
    }

    @PostMapping("/book")
    public ResponseEntity<?> bookMeals(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody MealBookingRequestDTO request
    ) {
        User user = new User();
        user.setEmail(userDetails.getUsername());

        mealBookingService.bookMeals(
                user,
                request.getBookingDates(),
                request.getLatitude(),
                request.getLongitude()
        );

        return ResponseEntity.ok(
                Map.of(
                        "message", "Meals booked successfully",
                        "dates", request.getBookingDates()
                )
        );
    }
}
