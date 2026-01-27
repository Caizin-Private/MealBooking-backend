package org.example.service;

public interface EmailService {
    void sendMealReportToHR(String reportContent);
    void sendTestEmail(String to);
}