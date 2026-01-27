package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "spring.task.scheduling", name = "enabled", havingValue = "true")
public class EmailTestController {

    private final EmailService emailService;

    @PostMapping("/send-test-email")
    public ResponseEntity<String> sendTestEmail(@RequestParam String email) {
        try {
            emailService.sendTestEmail(email);
            return ResponseEntity.ok("Test email sent successfully to " + email);
        } catch (Exception e) {
            log.error("Failed to send test email", e);
            return ResponseEntity.internalServerError().body("Failed to send test email: " + e.getMessage());
        }
    }

    @PostMapping("/send-hr-meal-report")
    public ResponseEntity<String> sendHRMealReport() {
        try {
            // Create a sample report for testing
            String sampleReport = "Sample HR Meal Report\n" +
                    "========================\n\n" +
                    "This is a test meal report generated at " + java.time.LocalDateTime.now() + "\n" +
                    "Total bookings would appear here in the actual implementation.\n\n" +
                    "This is a test email from the Smart Workplace Meal Booking System.";

            emailService.sendMealReportToHR(sampleReport);
            return ResponseEntity.ok("HR meal report sent successfully");
        } catch (Exception e) {
            log.error("Failed to send HR meal report", e);
            return ResponseEntity.internalServerError().body("Failed to send HR meal report: " + e.getMessage());
        }
    }
}
