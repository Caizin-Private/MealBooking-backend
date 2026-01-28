package org.example.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.BookingStatus;
import org.example.entity.MealBooking;
import org.example.repository.MealBookingRepository;
import org.example.service.EmailService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "spring.task.scheduling", name = "enabled", havingValue = "true")
@Profile("aws-email")
public class HrBookingSummaryScheduler {

    private final MealBookingRepository mealBookingRepository;
    private final EmailService emailService;

    @Scheduled(cron = "0 30 22 * * *", zone = "Asia/Kolkata")
    public void sendDailyBookingSummary() {
        try {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
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
}
