package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.MealBookingResponseDTO;
import org.example.dto.RangeMealBookingResponseDTO;
import org.example.dto.UpcomingMealsResponseDTO;
import org.example.dto.CancelMealRequestDTO;
import org.example.entity.*;
import org.example.repository.MealBookingRepository;
import org.example.repository.NotificationRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MealBookingServiceImpl implements MealBookingService {

    private final MealBookingRepository mealBookingRepository;
    private final UserRepository userRepository;
    private final PushNotificationService pushNotificationService;
    private final NotificationRepository notificationRepository;
    private final Clock clock;

    @Override
    public MealBookingResponseDTO bookSingleMeal(User user, LocalDate date) {
        try {
            LocalDate today = LocalDate.now(clock);
            if (date.isBefore(today)) {
                return MealBookingResponseDTO.failure("Cannot book meals for past dates");
            }

            if (date.getDayOfWeek().getValue() >= 6) {
                return MealBookingResponseDTO.failure("Cannot book meals on weekends (Saturday and Sunday)");
            }

            LocalTime now = LocalTime.now(clock);
            if (date.equals(today.plusDays(1)) && now.isAfter(LocalTime.of(22, 0))) {
                return MealBookingResponseDTO.failure("Booking closed for tomorrow after 10 PM");
            }

            if (mealBookingRepository.existsByUserAndBookingDate(user, date)) {
                return MealBookingResponseDTO.failure("Meal already booked for " + date);
            }

            MealBooking booking = MealBooking.builder()
                    .user(user)
                    .bookingDate(date)
                    .bookedAt(LocalDateTime.now(clock))
                    .status(BookingStatus.BOOKED)
                    .availableForLunch(true)
                    .build();

            MealBooking savedBooking = mealBookingRepository.save(booking);

            pushNotificationService.sendSingleMealBookingConfirmation(user.getId(), date);

            return MealBookingResponseDTO.success(
                    "Meal booked successfully for " + date,
                    savedBooking.getId(),
                    date.toString()
            );

        } catch (Exception e) {
            return MealBookingResponseDTO.failure("Booking failed: " + e.getMessage());
        }
    }

    @Override
    public RangeMealBookingResponseDTO bookRangeMeals(User user, LocalDate startDate, LocalDate endDate) {
        List<String> bookedDates = new ArrayList<>();
        List<String> failedBookings = new ArrayList<>();

        try {
            LocalDate today = LocalDate.now(clock);
            LocalTime now = LocalTime.now(clock);

            if (startDate.isBefore(today)) {
                return RangeMealBookingResponseDTO.failure("Cannot book meals for past dates", null);
            }

            if (endDate.isBefore(startDate)) {
                return RangeMealBookingResponseDTO.failure("End date cannot be before start date", null);
            }

            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                try {

                    if (date.getDayOfWeek().getValue() >= 6) {
                        failedBookings.add(date + " - Cannot book on weekends (Saturday and Sunday)");
                        continue;
                    }

                    if (date.equals(today.plusDays(1)) && now.isAfter(LocalTime.of(22, 0))) {
                        failedBookings.add(date + " - Booking closed after 10 PM");
                        continue;
                    }

                    if (mealBookingRepository.existsByUserAndBookingDate(user, date)) {
                        failedBookings.add(date + " - Already booked");
                        continue;
                    }

                    MealBooking booking = MealBooking.builder()
                            .user(user)
                            .bookingDate(date)
                            .bookedAt(LocalDateTime.now(clock))
                            .status(BookingStatus.BOOKED)
                            .availableForLunch(true)
                            .build();

                    mealBookingRepository.save(booking);
                    bookedDates.add(date.toString());

                } catch (Exception e) {
                    failedBookings.add(date + " - " + e.getMessage());
                }
            }

            if (!bookedDates.isEmpty()) {
                pushNotificationService.sendBookingConfirmation(user.getId(), startDate, endDate);
            }

            if (bookedDates.isEmpty()) {
                return RangeMealBookingResponseDTO.failure("No meals were booked", failedBookings);
            } else if (failedBookings.isEmpty()) {
                return RangeMealBookingResponseDTO.success(
                        "All meals booked successfully from " + startDate + " to " + endDate,
                        bookedDates
                );
            } else {
                return RangeMealBookingResponseDTO.success(
                        "Some meals booked successfully from " + startDate + " to " + endDate,
                        bookedDates
                );
            }

        } catch (Exception e) {
            return RangeMealBookingResponseDTO.failure("Range booking failed: " + e.getMessage(), failedBookings);
        }
    }

    @Override
    public UpcomingMealsResponseDTO getUpcomingMeals(User user) {
        LocalDate today = LocalDate.now(clock);
        List<MealBooking> allBookings = mealBookingRepository.findByUserOrderByBookingDateDesc(user);

        List<LocalDate> bookedDates = allBookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.BOOKED && !booking.getBookingDate().isBefore(today))
                .map(MealBooking::getBookingDate)
                .collect(Collectors.toList());

        return new UpcomingMealsResponseDTO(bookedDates);
    }

    @Override
    public MealBookingResponseDTO cancelMealByUserIdAndDate(CancelMealRequestDTO request) {
        try {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + request.getUserId()));

            LocalDate today = LocalDate.now(clock);
            LocalDate bookingDate = request.getBookingDate();

            if (bookingDate.isBefore(today)) {
                return MealBookingResponseDTO.failure("Cannot cancel meals for past dates");
            }

            MealBooking booking = mealBookingRepository.findByUserAndBookingDate(user, bookingDate)
                    .orElseThrow(() -> new RuntimeException("No booking found for user " + request.getUserId() + " on " + bookingDate));

            mealBookingRepository.delete(booking);

            Notification notification = Notification.builder()
                    .userId(user.getId())
                    .title("Meal Cancelled")
                    .message("Your meal booking for " + bookingDate + " has been cancelled successfully")
                    .type(NotificationType.CANCELLATION_CONFIRMATION)
                    .sent(false)
                    .scheduledAt(LocalDateTime.now(clock))
                    .sentAt(null)
                    .build();

            notificationRepository.save(notification);

            pushNotificationService.sendCancellationConfirmation(user.getId(), bookingDate);

            return MealBookingResponseDTO.success(
                    "Meal cancelled successfully for " + bookingDate,
                    booking.getId(),
                    bookingDate.toString()
            );

        } catch (Exception e) {
            return MealBookingResponseDTO.failure("Cancellation failed: " + e.getMessage());
        }
    }
}