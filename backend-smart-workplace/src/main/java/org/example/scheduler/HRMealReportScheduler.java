package org.example.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.BookingStatus;
import org.example.entity.MealBooking;
import org.example.repository.MealBookingRepository;
import org.example.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "spring.task.scheduling", name = "enabled", havingValue = "true")
public class HRMealReportScheduler {

    private final MealBookingRepository mealBookingRepository;
    private final EmailService emailService;
    private final Clock clock;

    @Value("${meal-booking.cutoff-time:22:00}")
    private String cutoffTimeConfig;

    @Scheduled(cron = "0 0 22 * * *", zone = "Asia/Kolkata") // 10 PM IST daily
    public void sendDailyMealReportToHR() {
        try {
            log.info("Starting HR meal report generation at 10 PM cutoff");

            // Check if current time is after cutoff
            LocalTime cutoffTime = LocalTime.parse(cutoffTimeConfig);
            LocalTime currentTime = LocalTime.now(clock);

            if (currentTime.isBefore(cutoffTime)) {
                log.info("Current time {} is before cutoff time {}, skipping report", currentTime, cutoffTime);
                return;
            }

            // Get tomorrow's bookings
            LocalDate tomorrow = LocalDate.now(clock).plusDays(1);

            // Skip weekends if needed (optional - you can configure this)
            if (tomorrow.getDayOfWeek().getValue() >= 6) {
                log.info("Tomorrow is weekend, skipping meal report");
                return;
            }

            List<MealBooking> tomorrowBookings = mealBookingRepository.findByBookingDateAndStatus(
                    tomorrow,
                    BookingStatus.BOOKED
            );

            if (tomorrowBookings.isEmpty()) {
                log.info("No meal bookings found for tomorrow: {}", tomorrow);
                sendEmptyReport(tomorrow);
                return;
            }

            String reportContent = generateReportContent(tomorrow, tomorrowBookings);
            emailService.sendMealReportToHR(reportContent);

            log.info("HR meal report sent successfully for {} bookings", tomorrowBookings.size());

        } catch (Exception e) {
            log.error("Failed to send HR meal report", e);
        }
    }

    private String generateReportContent(LocalDate date, List<MealBooking> bookings) {
        StringBuilder report = new StringBuilder();
        report.append("Daily Meal Booking Report for ").append(date).append("\n");
        report.append("=================================================\n\n");

        report.append("Total Bookings: ").append(bookings.size()).append("\n\n");

        report.append("Employee Details:\n");
        report.append("-----------------\n");

        bookings.stream()
                .sorted((b1, b2) -> b1.getUser().getName().compareToIgnoreCase(b2.getUser().getName()))
                .forEach(booking -> {
                    report.append("Name: ").append(booking.getUser().getName()).append("\n");
                    report.append("Email: ").append(booking.getUser().getEmail()).append("\n");
                    report.append("Booked At: ").append(booking.getBookedAt()).append("\n");
                    report.append("Available for Lunch: ").append(booking.getAvailableForLunch() ? "Yes" : "No").append("\n");
                    report.append("-----------------\n");
                });

        report.append("\nGenerated at: ").append(java.time.LocalDateTime.now(clock)).append("\n");
        report.append("This is an automated email from the Smart Workplace Meal Booking System.\n");

        return report.toString();
    }

    private void sendEmptyReport(LocalDate date) {
        String emptyReport = String.format(
                "Daily Meal Booking Report for %s\n" +
                        "=================================================\n\n" +
                        "Total Bookings: 0\n\n" +
                        "No employees have booked meals for tomorrow.\n\n" +
                        "Generated at: %s\n" +
                        "This is an automated email from the Smart Workplace Meal Booking System.\n",
                date,
                java.time.LocalDateTime.now(clock)
        );

        emailService.sendMealReportToHR(emptyReport);
        log.info("Empty meal report sent for {}", date);
    }
}