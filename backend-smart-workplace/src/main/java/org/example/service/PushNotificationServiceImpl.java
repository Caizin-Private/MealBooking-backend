package org.example.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PushNotificationServiceImpl implements PushNotificationService {

    @Override
    public void sendBookingConfirmation(Long userId, List<LocalDate> dates) {
        // TEMP: just log
        System.out.println(
                "Booking confirmed for user " + userId + " for dates " + dates
        );
    }
}