package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.config.AwsSesConfig;
import org.example.entity.MealBooking;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final SesClient sesClient;
    private final AwsSesConfig awsSesConfig;

    public void sendBookingSummaryEmail(List<MealBooking> bookings) {
        try {
            String subject = "Meal Booking Summary for Tomorrow - " +
                    bookings.get(0).getBookingDate().plusDays(1);

            String htmlBody = generateBookingSummaryHtml(bookings);
            String textBody = generateBookingSummaryText(bookings);

            SendEmailRequest emailRequest = SendEmailRequest.builder()
                    .destination(Destination.builder()
                            .toAddresses("misdivyanshi563@gmail.com") // HR email
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
                    .source("mdivyanshi563@gmail.com") // FROM email
                    .build();

            SendEmailResponse response = sesClient.sendEmail(emailRequest);
            log.info("Booking summary email sent successfully. Message ID: {}", response.messageId());

        } catch (Exception e) {
            log.error("Failed to send booking summary email. Error: {}", e.getMessage());
            log.error("Error details: ", e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    private String generateBookingSummaryHtml(List<MealBooking> bookings) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
                .append("<html><head><style>")
                .append("body { font-family: Arial, sans-serif; margin: 20px; }")
                .append("table { border-collapse: collapse; width: 100%; margin-top: 20px; }")
                .append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }")
                .append("th { background-color: #f2f2f2; }")
                .append(".summary { background-color: #e8f5e8; padding: 15px; border-radius: 5px; margin-bottom: 20px; }")
                .append("</style></head><body>")
                .append("<h2>🍽️ Meal Booking Summary</h2>")
                .append("<div class='summary'>")
                .append("<p><strong>Date:</strong> ").append(bookings.get(0).getBookingDate().plusDays(1)).append("</p>")
                .append("<p><strong>Total Bookings:</strong> ").append(bookings.size()).append("</p>")
                .append("</div>")
                .append("<h3>Booking Details:</h3>")
                .append("<table>")
                .append("<tr><th>Employee Name</th><th>Email</th><th>Booking Time</th><th>Status</th></tr>");

        for (MealBooking booking : bookings) {
            html.append("<tr>")
                    .append("<td>").append(booking.getUser().getName()).append("</td>")
                    .append("<td>").append(booking.getUser().getEmail()).append("</td>")
                    .append("<td>").append(booking.getBookedAt().toLocalTime()).append("</td>")
                    .append("<td>").append(booking.getStatus()).append("</td>")
                    .append("</tr>");
        }

        html.append("</table>")
                .append("<p style='margin-top: 20px; font-size: 12px; color: #666;'>")
                .append("This is an automated email from the Smart Workplace Meal Booking System.")
                .append("</p>")
                .append("</body></html>");

        return html.toString();
    }

    private String generateBookingSummaryText(List<MealBooking> bookings) {
        StringBuilder text = new StringBuilder();
        text.append("MEAL BOOKING SUMMARY\n");
        text.append("====================\n\n");
        text.append("Date: ").append(bookings.get(0).getBookingDate().plusDays(1)).append("\n");
        text.append("Total Bookings: ").append(bookings.size()).append("\n\n");
        text.append("BOOKING DETAILS:\n");
        text.append("-----------------\n");

        for (MealBooking booking : bookings) {
            text.append("Employee: ").append(booking.getUser().getName())
                    .append(" (").append(booking.getUser().getEmail()).append(")\n");
            text.append("Booking Time: ").append(booking.getBookedAt().toLocalTime()).append("\n");
            text.append("Status: ").append(booking.getStatus()).append("\n");
            text.append("---\n");
        }

        text.append("\nThis is an automated email from the Smart Workplace Meal Booking System.");

        return text.toString();
    }
}
