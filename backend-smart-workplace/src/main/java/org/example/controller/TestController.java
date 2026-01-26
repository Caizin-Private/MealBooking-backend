package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.CancelMealRequestDTO;
import org.example.dto.LocationUpdateRequestDTO;
import org.example.dto.UpcomingMealsRequestDTO;
import org.example.entity.Notification;
import org.example.entity.User;
import org.example.repository.NotificationRepository;
import org.example.repository.UserRepository;
import org.example.scheduler.MealInactivityScheduler;
import org.example.scheduler.MealReminderScheduler;
import org.example.scheduler.NotificationSenderScheduler;
import org.example.service.MealBookingService;
import org.example.service.UserLocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Tag(name = "Test Controller", description = "Test endpoints for debugging schedulers")
@Slf4j
public class TestController {

    private final MealInactivityScheduler mealInactivityScheduler;
    private final NotificationSenderScheduler notificationSenderScheduler;
    private final MealReminderScheduler mealReminderScheduler;
    private final MealBookingService mealBookingService;
    private final UserLocationService userLocationService;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    @PostMapping("/trigger-inactivity-scheduler")
    @Operation(summary = "Manually trigger MealInactivityScheduler", description = "For testing purposes only")
    public ResponseEntity<Map<String, Object>> triggerInactivityScheduler() {
        try {
            log.info("Manually triggering MealInactivityScheduler...");
            mealInactivityScheduler.sendInactivityNudges();

            // Check notifications created
            List<Notification> notifications = notificationRepository.findAll();
            long inactivityNotifications = notifications.stream()
                    .filter(n -> n.getType() == org.example.entity.NotificationType.INACTIVITY_NUDGE)
                    .count();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Inactivity scheduler triggered successfully",
                    "totalNotifications", notifications.size(),
                    "inactivityNotifications", inactivityNotifications,
                    "notifications", notifications
            ));
        } catch (Exception e) {
            log.error("Error triggering inactivity scheduler", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/trigger-meal-reminder-scheduler")
    @Operation(summary = "Manually trigger MealReminderScheduler", description = "For testing purposes only")
    public ResponseEntity<Map<String, Object>> triggerMealReminderScheduler() {
        try {
            log.info("Manually triggering MealReminderScheduler...");
            mealReminderScheduler.sendMealBookingReminders();

            // Check notifications created
            List<Notification> notifications = notificationRepository.findAll();
            long reminderNotifications = notifications.stream()
                    .filter(n -> n.getType() == org.example.entity.NotificationType.MEAL_REMINDER)
                    .count();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Meal reminder scheduler triggered successfully",
                    "totalNotifications", notifications.size(),
                    "reminderNotifications", reminderNotifications,
                    "notifications", notifications
            ));
        } catch (Exception e) {
            log.error("Error triggering meal reminder scheduler", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/trigger-notification-sender")
    @Operation(summary = "Manually trigger NotificationSenderScheduler", description = "For testing purposes only")
    public ResponseEntity<Map<String, Object>> triggerNotificationSender() {
        try {
            log.info("Manually triggering NotificationSenderScheduler...");
            notificationSenderScheduler.sendPendingNotifications();

            // Check notifications status
            List<Notification> notifications = notificationRepository.findAll();
            long sentNotifications = notifications.stream()
                    .filter(n -> n.isSent())
                    .count();
            long pendingNotifications = notifications.stream()
                    .filter(n -> !n.isSent())
                    .count();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Notification sender triggered successfully",
                    "totalNotifications", notifications.size(),
                    "sentNotifications", sentNotifications,
                    "pendingNotifications", pendingNotifications,
                    "notifications", notifications
            ));
        } catch (Exception e) {
            log.error("Error triggering notification sender", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/create-user")
    @Operation(summary = "Create a test user", description = "Creates a test user for testing purposes")
    public ResponseEntity<User> createTestUser() {
        try {
            User user = User.builder()
                    .name("Test User")
                    .email("test.user@example.com")
                    .role(org.example.entity.Role.USER)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();

            User savedUser = userRepository.save(user);
            return ResponseEntity.ok(savedUser);
        } catch (Exception e) {
            log.error("Error creating test user", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/users")
    @Operation(summary = "Get all test users", description = "Returns all users in the system")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/notifications")
    @Operation(summary = "Get all notifications for testing", description = "Returns all notifications in the system")
    public ResponseEntity<List<Notification>> getAllNotifications() {
        return ResponseEntity.ok(notificationRepository.findAll());
    }

    @DeleteMapping("/cleanup")
    @Operation(summary = "Clean up test data", description = "Deletes all test users and notifications")
    public ResponseEntity<Map<String, String>> cleanupTestData() {
        try {
            // Delete all notifications
            notificationRepository.deleteAll();

            // Delete users with email containing "test"
            List<User> testUsers = userRepository.findAll().stream()
                    .filter(u -> u.getEmail().contains("test"))
                    .toList();
            userRepository.deleteAll(testUsers);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Test data cleaned up successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/book-single-meal")
    @Operation(summary = "Book single meal for testing", description = "Book meal for a specific date for test user")
    public ResponseEntity<Map<String, Object>> bookSingleMeal(@RequestParam Long userId, @RequestParam String date) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

            java.time.LocalDate bookingDate = java.time.LocalDate.parse(date);

            // Create request DTO without userId
            org.example.dto.SingleMealBookingRequestDTO request = new org.example.dto.SingleMealBookingRequestDTO();
            request.setDate(bookingDate);

            var response = mealBookingService.bookSingleMeal(user, bookingDate);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Single meal booked successfully",
                    "response", response
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/book-range-meals")
    @Operation(summary = "Book range meals for testing", description = "Book meals for date range for test user")
    public ResponseEntity<Map<String, Object>> bookRangeMeals(@RequestParam Long userId, @RequestParam String startDate, @RequestParam String endDate) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

            java.time.LocalDate start = java.time.LocalDate.parse(startDate);
            java.time.LocalDate end = java.time.LocalDate.parse(endDate);

            // Create request DTO without userId
            org.example.dto.RangeMealBookingRequestDTO request = new org.example.dto.RangeMealBookingRequestDTO();
            request.setStartDate(start);
            request.setEndDate(end);

            var response = mealBookingService.bookRangeMeals(user, start, end);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Range meals booked successfully",
                    "response", response
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/cancel-meal")
    @Operation(summary = "Cancel meal for testing", description = "Cancel meal for a specific date for test user")
    public ResponseEntity<Map<String, Object>> cancelMeal(@RequestParam Long userId, @RequestParam String date) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

            java.time.LocalDate bookingDate = java.time.LocalDate.parse(date);

            // Create request DTO without userId
            CancelMealRequestDTO request = new CancelMealRequestDTO();
            request.setBookingDate(bookingDate);

            var response = mealBookingService.cancelMealByUserIdAndDate(user, request);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Meal cancelled successfully",
                    "response", response
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/upcoming-meals")
    @Operation(summary = "Get upcoming meals for testing", description = "Get upcoming meals for test user")
    public ResponseEntity<Map<String, Object>> getUpcomingMeals(@RequestParam Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

            // Create request DTO without userId
            UpcomingMealsRequestDTO request = new UpcomingMealsRequestDTO();

            var response = mealBookingService.getUpcomingMeals(user);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Upcoming meals retrieved successfully",
                    "response", response
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/update-location")
    @Operation(summary = "Update location for testing", description = "Update user location for frontend integration testing")
    public ResponseEntity<Map<String, Object>> updateLocation(@RequestParam Long userId, @RequestParam Double latitude, @RequestParam Double longitude) {
        try {
            LocationUpdateRequestDTO request = LocationUpdateRequestDTO.builder()
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();

            userLocationService.saveLocation(userId, request);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Location updated successfully",
                    "userId", userId,
                    "latitude", latitude,
                    "longitude", longitude
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
}
