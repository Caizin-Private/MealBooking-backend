package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.BookingStatus;
import org.example.entity.MealBooking;
import org.example.repository.MealBookingRepository;
import org.example.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Slf4j
public class EmailTestController {

    private final EmailService emailService;
    private final MealBookingRepository mealBookingRepository;

    @PostMapping("/test-booking-summary")
    public ResponseEntity<String> testBookingSummaryEmail(@RequestParam(required = false) String date) {
        try {
            LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now().plusDays(1);

            List<MealBooking> bookings = mealBookingRepository.findByBookingDateAndStatus(
                    targetDate,
                    BookingStatus.BOOKED
            );

            if (bookings.isEmpty()) {
                return ResponseEntity.ok("No bookings found for " + targetDate + ". Email would indicate no bookings.");
            }

            emailService.sendBookingSummaryEmail(bookings);
            return ResponseEntity.ok("Test booking summary email sent for " + targetDate +
                    " with " + bookings.size() + " bookings.");

        } catch (Exception e) {
            log.error("Error sending test booking summary email", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to send test email: " + e.getMessage());
        }
    }

    @GetMapping("/booking-count")
    public ResponseEntity<String> getBookingCount(@RequestParam(required = false) String date) {
        try {
            LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now().plusDays(1);

            List<MealBooking> bookings = mealBookingRepository.findByBookingDateAndStatus(
                    targetDate,
                    BookingStatus.BOOKED
            );

            return ResponseEntity.ok("Bookings for " + targetDate + ": " + bookings.size());

        } catch (Exception e) {
            log.error("Error getting booking count", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to get booking count: " + e.getMessage());
        }
    }
}
