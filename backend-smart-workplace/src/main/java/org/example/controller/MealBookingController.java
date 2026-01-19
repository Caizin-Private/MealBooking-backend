package org.example.controller;

import org.example.dto.MealBookingRequestDTO;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.example.service.MealBookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meals")
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
    public ResponseEntity<?> bookMeals(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody MealBookingRequestDTO request
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        mealBookingService.bookMeals(
                user,
                request.getStartDate(),
                request.getEndDate(),
                request.getLatitude(),
                request.getLongitude()
        );

        return ResponseEntity.ok("Meals booked successfully");
    }
}
