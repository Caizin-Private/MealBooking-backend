package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.MealBooking;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final SesClient sesClient;

    @Value("${aws.ses.from-email}")
    private String fromEmail;

    @Value("${aws.ses.hr-email}")
    private String hrEmail;

    public void sendBookingSummaryEmail(List<MealBooking> bookings) {
        if (bookings == null || bookings.isEmpty()) {
            log.warn("No bookings found. Skipping booking summary email.");
            return;
        }

        try {
            String subject = "Meal Booking Summary for " +
                    bookings.get(0).getBookingDate().plusDays(1);

            String htmlBody = generateBookingSummaryHtml(bookings);
            String textBody = generateBookingSummaryText(bookings);

            SendEmailRequest emailRequest = SendEmailRequest.builder()
                    .destination(Destination.builder()
                            .toAddresses(hrEmail)
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .data(subject)
                                    .charset("UTF-8")
                                    .build())
                            .body(Body.builder()
                                    .html(Content.builder()
                                            .data(htmlBody)
                                            .charset("UTF-8")
                                            .build())
                                    .text(Content.builder()
                                            .data(textBody)
                                            .charset("UTF-8")
                                            .build())
                                    .build())
                            .build())
                    .source(fromEmail)
                    .build();

            SendEmailResponse response = sesClient.sendEmail(emailRequest);
            log.info("Booking summary email sent. MessageId={}", response.messageId());

        } catch (SesException e) {
            log.error("AWS SES error while sending booking summary email", e);
            throw new RuntimeException("AWS SES error: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while sending booking summary email", e);
            throw new RuntimeException("Failed to send booking summary email", e);
        }
    }

    // ---------- Helpers ----------

    private String generateBookingSummaryHtml(List<MealBooking> bookings) {
        StringBuilder html = new StringBuilder();

        html.append("<html><body style='font-family:Arial;'>")
                .append("<h2>🍽️ Meal Booking Summary</h2>")
                .append("<p><strong>Date:</strong> ")
                .append(bookings.get(0).getBookingDate().plusDays(1))
                .append("</p>")
                .append("<p><strong>Total Bookings:</strong> ")
                .append(bookings.size())
                .append("</p>")
                .append("<table border='1' cellpadding='8' cellspacing='0'>")
                .append("<tr><th>Name</th><th>Email</th><th>Time</th><th>Status</th></tr>");

        for (MealBooking booking : bookings) {
            html.append("<tr>")
                    .append("<td>").append(booking.getUser().getName()).append("</td>")
                    .append("<td>").append(booking.getUser().getEmail()).append("</td>")
                    .append("<td>").append(booking.getBookedAt().toLocalTime()).append("</td>")
                    .append("<td>").append(booking.getStatus()).append("</td>")
                    .append("</tr>");
        }

        html.append("</table>")
                .append("<p style='font-size:12px;color:#666'>Automated email from Smart Workplace</p>")
                .append("</body></html>");

        return html.toString();
    }

    private String generateBookingSummaryText(List<MealBooking> bookings) {
        StringBuilder text = new StringBuilder();

        text.append("MEAL BOOKING SUMMARY\n")
                .append("Date: ")
                .append(bookings.get(0).getBookingDate().plusDays(1))
                .append("\nTotal Bookings: ")
                .append(bookings.size())
                .append("\n\n");

        for (MealBooking booking : bookings) {
            text.append("Employee: ")
                    .append(booking.getUser().getName())
                    .append(" (")
                    .append(booking.getUser().getEmail())
                    .append(")\n")
                    .append("Time: ")
                    .append(booking.getBookedAt().toLocalTime())
                    .append("\nStatus: ")
                    .append(booking.getStatus())
                    .append("\n---\n");
        }

        return text.toString();
    }
}
