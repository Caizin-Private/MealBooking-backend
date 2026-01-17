package org.example.service;

import java.time.LocalDate;
import java.util.List;

public interface PushNotificationService {

    // We will implement this later
    void sendBookingConfirmation(Long userId, List<LocalDate> bookingDates);
}