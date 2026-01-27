package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;
import software.amazon.awssdk.services.ses.model.Content;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Value("${aws.ses.from-email}")
    private String fromEmail;

    @Value("${aws.ses.hr-email}")
    private String hrEmail;
    @Value("${aws.access-key}")
    private String accessKey;

    @Value("${aws.secret-key}")
    private String secretKey;

    @Value("${aws.region}")
    private String region;

    @Override
    public void sendMealReportToHR(String reportContent) {
        try {
            SesClient sesClient = createSesClient();

            SendEmailRequest emailRequest = SendEmailRequest.builder()
                    .destination(Destination.builder()
                            .toAddresses(hrEmail)
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .data("Daily Meal Booking Report")
                                    .charset("UTF-8")
                                    .build())
                            .body(Body.builder()
                                    .text(Content.builder()
                                            .data(reportContent)
                                            .charset("UTF-8")
                                            .build())
                                    .build())
                            .build())
                    .source(fromEmail)
                    .build();

            SendEmailResponse response = sesClient.sendEmail(emailRequest);
            log.info("HR meal report sent successfully! Message ID: {}", response.messageId());

        } catch (Exception e) {
            log.error("Failed to send HR meal report", e);
            throw new RuntimeException("Failed to send email report", e);
        }
    }

    @Override
    public void sendTestEmail(String to) {
        try {
            SesClient sesClient = createSesClient();

            SendEmailRequest emailRequest = SendEmailRequest.builder()
                    .destination(Destination.builder()
                            .toAddresses(to)
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .data("Test Email from Meal Booking System")
                                    .charset("UTF-8")
                                    .build())
                            .body(Body.builder()
                                    .text(Content.builder()
                                            .data("This is a test email from the meal booking system.")
                                            .charset("UTF-8")
                                            .build())
                                    .build())
                            .build())
                    .source(fromEmail)
                    .build();

            SendEmailResponse response = sesClient.sendEmail(emailRequest);
            log.info("Test email sent successfully! Message ID: {}", response.messageId());

        } catch (Exception e) {
            log.error("Failed to send test email", e);
            throw new RuntimeException("Failed to send test email", e);
        }
    }

    private SesClient createSesClient() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        return SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}
