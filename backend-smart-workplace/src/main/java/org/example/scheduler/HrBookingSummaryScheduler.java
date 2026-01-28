package org.example.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.BookingStatus;
import org.example.entity.MealBooking;
import org.example.repository.MealBookingRepository;
import org.example.service.EmailService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "spring.task.scheduling", name = "enabled", havingValue = "true")
@Profile("aws-email")
public class HrBookingSummaryScheduler {

    private final MealBookingRepository mealBookingRepository;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 16 * * *", zone = "Asia/Kolkata") // 4 PM IST daily
    public void sendDailyBookingSummary() {
        try {
            LocalDate tomorrow = LocalDate.now().plusDays(1);

            // Skip weekends - no need to send summary for Saturday/Sunday
            if (tomorrow.getDayOfWeek().getValue() >= 6) {
                log.info("Skipping HR booking summary for weekend date: {}", tomorrow);
                return;
            }

            List<MealBooking> bookings = mealBookingRepository.findByBookingDateAndStatus(
                    tomorrow,
                    BookingStatus.BOOKED
            );

            log.info("Found {} bookings for tomorrow ({})", bookings.size(), tomorrow);

            if (bookings.isEmpty()) {
                log.info("No bookings found for tomorrow. Sending empty summary email to HR.");
            } else {
                log.info("Sending booking summary email to HR for {} bookings", bookings.size());
            }

            emailService.sendBookingSummaryEmail(bookings);
            log.info("HR booking summary email sent successfully for {}", tomorrow);

        } catch (Exception e) {
            log.error("Failed to send HR booking summary email", e);
        }
    }

    @Scheduled(cron = "0 30 22 * * *", zone = "Asia/Kolkata") // 10:30 PM IST daily - after cutoff
    public void sendFinalBookingCount() {
        try {
            LocalDate tomorrow = LocalDate.now().plusDays(1);

            // Skip weekends
            if (tomorrow.getDayOfWeek().getValue() >= 6) {
                log.info("Skipping final HR booking count for weekend date: {}", tomorrow);
                return;
            }

            List<MealBooking> bookings = mealBookingRepository.findByBookingDateAndStatus(
                    tomorrow,
                    BookingStatus.BOOKED
            );

            log.info("Final booking count for tomorrow ({}): {} bookings", tomorrow, bookings.size());

            // You could implement a different email template for final count
            // For now, using the same summary email
            emailService.sendBookingSummaryEmail(bookings);
            log.info("Final HR booking summary email sent for {}", tomorrow);

        } catch (Exception e) {
            log.error("Failed to send final HR booking summary email", e);
        }
    }
}
