package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.SingleMealBookingResponseDTO;
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
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final Clock clock;

    @Override
    public SingleMealBookingResponseDTO bookSingleMeal(User user, LocalDate date) {
        try {
            LocalDate today = LocalDate.now(clock);
            if (date.isBefore(today)) {
                return SingleMealBookingResponseDTO.failure("Cannot book meals for past dates");
            }

            if (date.getDayOfWeek().getValue() >= 6) {
                return SingleMealBookingResponseDTO.failure("Cannot book meals on weekends (Saturday and Sunday)");
            }

            LocalTime now = LocalTime.now(clock);
            if (date.equals(today.plusDays(1)) && now.isAfter(LocalTime.of(22, 0))) {
                return SingleMealBookingResponseDTO.failure("Booking closed for tomorrow after 10 PM");
            }

            // Check if booking already exists with BOOKED status
            boolean existingBookedMeal = mealBookingRepository.existsByUserAndBookingDateAndStatus(user, date, BookingStatus.BOOKED);
            if (existingBookedMeal) {
                return SingleMealBookingResponseDTO.failure("Meal already booked for " + date);
            }

            // Check if there's a cancelled booking to reactivate
            MealBooking cancelledBooking = mealBookingRepository.findByUserAndBookingDateAndStatus(user, date, BookingStatus.CANCELLED)
                    .orElse(null);

            if (cancelledBooking != null) {
                // Apply cutoff time check for rebooking too
                if (date.equals(today.plusDays(1)) && now.isAfter(LocalTime.of(22, 0))) {
                    return SingleMealBookingResponseDTO.failure("Rebooking closed for tomorrow after 10 PM");
                }

                // Reactivate cancelled booking
                cancelledBooking.setStatus(BookingStatus.BOOKED);
                cancelledBooking.setBookedAt(LocalDateTime.now(clock));
                cancelledBooking.setAvailableForLunch(false);

                MealBooking reactivatedBooking = mealBookingRepository.save(cancelledBooking);

                notificationService.createAndSendImmediately(
                        user.getId(),
                        "Meal rebooked",
                        "Your cancelled meal has been rebooked for " + date,
                        NotificationType.BOOKING_CONFIRMATION
                );

                return SingleMealBookingResponseDTO.success(
                        "Meal rebooked successfully for " + date,
                        date.toString()
                );
            }

            MealBooking booking = MealBooking.builder()
                    .user(user)
                    .bookingDate(date)
                    .bookedAt(LocalDateTime.now(clock))
                    .status(BookingStatus.BOOKED)
                    .availableForLunch(false)
                    .build();

            MealBooking savedBooking = mealBookingRepository.save(booking);

            notificationService.createAndSendImmediately(
                    user.getId(),
                    "Meal booked",
                    "Your meal has been booked for " + date,
                    NotificationType.BOOKING_CONFIRMATION
            );

            return SingleMealBookingResponseDTO.success(
                    "Meal booked successfully for " + date,
                    date.toString()
            );

        } catch (Exception e) {
            return SingleMealBookingResponseDTO.failure("Booking failed: " + e.getMessage());
        }
    }

    @Override
    public RangeMealBookingResponseDTO bookRangeMeals(User user, LocalDate startDate, LocalDate endDate) {

        List<String> bookedDates = new ArrayList<>();

        try {
            LocalDate today = LocalDate.now(clock);
            LocalTime now = LocalTime.now(clock);

            if (startDate.isBefore(today)) {
                return RangeMealBookingResponseDTO.failure("Cannot book meals for past dates");
            }

            if (endDate.isBefore(startDate)) {
                return RangeMealBookingResponseDTO.failure("End date cannot be before start date");
            }

            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {

                // Skip weekends silently
                if (date.getDayOfWeek().getValue() >= 6) {
                    continue;
                }

                // Cutoff check (tomorrow after 10 PM)
                if (date.equals(today.plusDays(1)) && now.isAfter(LocalTime.of(22, 0))) {
                    continue;
                }

                // Check if already booked - skip if so
                boolean existingBookedMeal = mealBookingRepository.existsByUserAndBookingDateAndStatus(user, date, BookingStatus.BOOKED);
                if (existingBookedMeal) {
                    continue;
                }

                // Check if there's a cancelled booking to reactivate
                MealBooking cancelledBooking = mealBookingRepository.findByUserAndBookingDateAndStatus(user, date, BookingStatus.CANCELLED)
                        .orElse(null);

                if (cancelledBooking != null) {
                    // Reactivate cancelled booking
                    cancelledBooking.setStatus(BookingStatus.BOOKED);
                    cancelledBooking.setBookedAt(LocalDateTime.now(clock));
                    cancelledBooking.setAvailableForLunch(false);
                    mealBookingRepository.save(cancelledBooking);
                    bookedDates.add(date.toString());
                    continue;
                }

                // Create new booking if none exists
                MealBooking booking = MealBooking.builder()
                        .user(user)
                        .bookingDate(date)
                        .bookedAt(LocalDateTime.now(clock))
                        .status(BookingStatus.BOOKED)
                        .availableForLunch(false)
                        .build();

                mealBookingRepository.save(booking);
                bookedDates.add(date.toString());
            }

            if (bookedDates.isEmpty()) {
                return RangeMealBookingResponseDTO.failure(
                        "No meals were booked in the selected range"
                );
            }

            notificationService.createAndSendImmediately(
                    user.getId(),
                    "Meals booked",
                    "Meals booked from " + startDate + " to " + endDate,
                    NotificationType.BOOKING_CONFIRMATION
            );


            return RangeMealBookingResponseDTO.success(
                    "Meals booked successfully from " + startDate + " to " + endDate,
                    bookedDates
            );

        } catch (Exception e) {
            return RangeMealBookingResponseDTO.failure(
                    "Range booking failed: " + e.getMessage()
            );
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
    public SingleMealBookingResponseDTO cancelMealByUserIdAndDate(User user, CancelMealRequestDTO request) {
        try {
            LocalDate today = LocalDate.now(clock);
            LocalDate bookingDate = request.getBookingDate();
            LocalTime now = LocalTime.now(clock);

            if (bookingDate.isBefore(today)) {
                return SingleMealBookingResponseDTO.failure("Cannot cancel meals for past dates");
            }

            // Apply cutoff time check for cancellation too
            if (bookingDate.equals(today.plusDays(1)) && now.isAfter(LocalTime.of(22, 0))) {
                return SingleMealBookingResponseDTO.failure("Cancellation closed for tomorrow after 10 PM");
            }

            MealBooking booking = mealBookingRepository.findByUserAndBookingDate(user, bookingDate)
                    .orElseThrow(() -> new RuntimeException("No booking found for user " + user.getId() + " on " + bookingDate));

            if (booking.getStatus() != BookingStatus.BOOKED) {
                return SingleMealBookingResponseDTO.failure("Cannot cancel meal that is not booked. Current status: " + booking.getStatus());
            }

            booking.setStatus(BookingStatus.CANCELLED);
            mealBookingRepository.save(booking);

            notificationService.createAndSendImmediately(
                    user.getId(),
                    "Meal Cancelled",
                    "Your meal booking for " + bookingDate + " has been cancelled successfully",
                    NotificationType.CANCELLATION_CONFIRMATION
            );


            return SingleMealBookingResponseDTO.success(
                    "Meal cancelled successfully for " + bookingDate,
                    bookingDate.toString()
            );

        } catch (Exception e) {
            return SingleMealBookingResponseDTO.failure("Cancellation failed: " + e.getMessage());
        }
    }
}