package org.example.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class PushNotificationServiceImpl implements PushNotificationService {

    @Override
    public void sendBookingConfirmation(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        // TEMP: just log (later → email / push / SMS)
        System.out.println(
                "Meal booking confirmed for user " + userId +
                        " from " + startDate +
                        " to " + endDate
        );
    }
}
