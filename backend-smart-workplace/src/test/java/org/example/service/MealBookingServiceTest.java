
package org.example.service;

import org.example.dto.SingleMealBookingResponseDTO;
import org.example.dto.RangeMealBookingResponseDTO;
import org.example.dto.UpcomingMealsResponseDTO;
import org.example.dto.CancelMealRequestDTO;
import org.example.entity.BookingStatus;
import org.example.entity.MealBooking;
import org.example.entity.NotificationType;
import org.example.entity.User;
import org.example.repository.MealBookingRepository;
import org.example.repository.NotificationRepository;
import org.example.repository.UserRepository;
import org.example.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MealBookingServiceTest {

    @Mock
    private MealBookingRepository mealBookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PushNotificationService pushNotificationService;

    @Mock
    private Clock clock;

    @InjectMocks
    private MealBookingServiceImpl mealBookingService;

    private User testUser;
    private LocalDate today;
    private LocalDate tomorrow;
    private LocalDate nextWeek;

    @BeforeEach
    void setUp() {
        testUser = new User(1L, "Test User", "test@example.com", org.example.entity.Role.USER, LocalDateTime.now());

        // Set up fixed clock for predictable tests
        Clock fixedClock = Clock.fixed(LocalDate.of(2026, 1, 25).atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        ReflectionTestUtils.setField(mealBookingService, "clock", fixedClock);

        today = LocalDate.now(fixedClock);
        tomorrow = today.plusDays(1);
        nextWeek = today.plusDays(7);
    }

    @Test
    void shouldBookSingleMealSuccessfully() {
        // Given
        when(mealBookingRepository.existsByUserAndBookingDateAndStatus(testUser, tomorrow, BookingStatus.BOOKED))
                .thenReturn(false);
        when(mealBookingRepository.findByUserAndBookingDateAndStatus(testUser, tomorrow, BookingStatus.CANCELLED))
                .thenReturn(Optional.empty());
        when(mealBookingRepository.save(any(MealBooking.class))).thenAnswer(invocation -> {
            MealBooking booking = invocation.getArgument(0);
            booking.setId(123L);
            return booking;
        });

        // When
        SingleMealBookingResponseDTO response = mealBookingService.bookSingleMeal(testUser, tomorrow);

        // Then
        assertTrue(response.getMessage().contains("successfully"));
        assertEquals("Meal booked successfully for " + tomorrow, response.getMessage());
        assertEquals(tomorrow.toString(), response.getBookingDate());
        verify(notificationService).createAndSendImmediately(
                testUser.getId(),
                "Meal booked",
                "Your meal has been booked for " + tomorrow,
                NotificationType.BOOKING_CONFIRMATION
        );
    }

    @Test
    void shouldFailToBookSingleMealForPastDate() {
        // When
        SingleMealBookingResponseDTO response = mealBookingService.bookSingleMeal(testUser, today.minusDays(1));

        // Then
        assertFalse(response.getMessage().contains("successfully"));
        assertEquals("Cannot book meals for past dates", response.getMessage());
        verifyNoInteractions(pushNotificationService);
    }

    @Test
    void shouldFailToBookSingleMealForWeekend() {
        // Given - Find next Saturday (day 6)
        LocalDate saturday = today;
        while (saturday.getDayOfWeek().getValue() != 6) {
            saturday = saturday.plusDays(1);
        }

        // When
        SingleMealBookingResponseDTO response = mealBookingService.bookSingleMeal(testUser, saturday);

        // Then
        assertFalse(response.getMessage().contains("successfully"));
        assertEquals("Cannot book meals on weekends (Saturday and Sunday)", response.getMessage());
        verifyNoInteractions(pushNotificationService);
    }

    @Test
    void shouldFailToBookSingleMealAfterCutoffTime() {
        // Given - Set clock to after 10 PM for tomorrow's booking
        Clock lateNightClock = Clock.fixed(
                LocalDate.of(2026, 1, 25)
                        .atTime(22, 30) // 10:30 PM
                        .atZone(ZoneId.systemDefault())
                        .toInstant(),
                ZoneId.systemDefault()
        );
        ReflectionTestUtils.setField(mealBookingService, "clock", lateNightClock);

        // When
        SingleMealBookingResponseDTO response = mealBookingService.bookSingleMeal(
                testUser,
                LocalDate.of(2026, 1, 26) // Tomorrow
        );

        // Then
        assertFalse(response.getMessage().contains("successfully"));
        assertEquals("Booking closed for tomorrow after 10 PM", response.getMessage());
        verifyNoInteractions(notificationService);
        verifyNoInteractions(pushNotificationService);
    }

    @Test
    void shouldFailToBookSingleMealForDuplicateBooking() {
        // Given
        when(mealBookingRepository.existsByUserAndBookingDateAndStatus(testUser, tomorrow, BookingStatus.BOOKED))
                .thenReturn(true);

        // When
        SingleMealBookingResponseDTO response = mealBookingService.bookSingleMeal(testUser, tomorrow);

        // Then
        assertFalse(response.getMessage().contains("successfully"));
        assertEquals("Meal already booked for " + tomorrow, response.getMessage());
        verifyNoInteractions(pushNotificationService);
    }

    @Test
    void shouldRebookCancelledMealSuccessfully() {
        // Given
        MealBooking cancelledBooking = MealBooking.builder()
                .id(1L)
                .user(testUser)
                .bookingDate(tomorrow)
                .status(BookingStatus.CANCELLED)
                .build();

        when(mealBookingRepository.existsByUserAndBookingDateAndStatus(testUser, tomorrow, BookingStatus.BOOKED))
                .thenReturn(false);
        when(mealBookingRepository.findByUserAndBookingDateAndStatus(testUser, tomorrow, BookingStatus.CANCELLED))
                .thenReturn(Optional.of(cancelledBooking));
        when(mealBookingRepository.save(any(MealBooking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SingleMealBookingResponseDTO response = mealBookingService.bookSingleMeal(testUser, tomorrow);

        // Then
        assertTrue(response.getMessage().contains("successfully"));
        assertEquals("Meal rebooked successfully for " + tomorrow, response.getMessage());
        assertEquals(tomorrow.toString(), response.getBookingDate());
        verify(notificationService).createAndSendImmediately(
                testUser.getId(),
                "Meal rebooked",
                "Your cancelled meal has been rebooked for " + tomorrow,
                NotificationType.BOOKING_CONFIRMATION
        );
    }

    @Test
    void shouldFailToRebookCancelledMealAfterCutoffTime() {
        // Given - Set clock to after 10 PM for tomorrow's booking
        Clock lateNightClock = Clock.fixed(
                LocalDate.of(2026, 1, 25)
                        .atTime(22, 30) // 10:30 PM
                        .atZone(ZoneId.systemDefault())
                        .toInstant(),
                ZoneId.systemDefault()
        );
        ReflectionTestUtils.setField(mealBookingService, "clock", lateNightClock);

        MealBooking cancelledBooking = MealBooking.builder()
                .id(1L)
                .user(testUser)
                .bookingDate(LocalDate.of(2026, 1, 26)) // Tomorrow
                .status(BookingStatus.CANCELLED)
                .build();

        when(mealBookingRepository.existsByUserAndBookingDateAndStatus(testUser, LocalDate.of(2026, 1, 26), BookingStatus.BOOKED))
                .thenReturn(false);
        when(mealBookingRepository.findByUserAndBookingDateAndStatus(testUser, LocalDate.of(2026, 1, 26), BookingStatus.CANCELLED))
                .thenReturn(Optional.of(cancelledBooking));

        // When
        SingleMealBookingResponseDTO response = mealBookingService.bookSingleMeal(
                testUser,
                LocalDate.of(2026, 1, 26)
        );

        // Then
        assertFalse(response.getMessage().contains("successfully"));
        assertEquals("Rebooking closed for tomorrow after 10 PM", response.getMessage());
        verifyNoInteractions(notificationService);
        verifyNoInteractions(pushNotificationService);
    }

    @Test
    void shouldBookRangeMealsSuccessfully() {
        // Given
        LocalDate startDate = tomorrow;
        LocalDate endDate = tomorrow.plusDays(2);

        when(mealBookingRepository.existsByUserAndBookingDateAndStatus(eq(testUser), any(LocalDate.class), eq(BookingStatus.BOOKED)))
                .thenReturn(false);
        when(mealBookingRepository.findByUserAndBookingDateAndStatus(eq(testUser), any(LocalDate.class), eq(BookingStatus.CANCELLED)))
                .thenReturn(Optional.empty());
        when(mealBookingRepository.save(any(MealBooking.class))).thenAnswer(invocation -> {
            MealBooking booking = invocation.getArgument(0);
            booking.setId(System.currentTimeMillis());
            return booking;
        });

        // When
        RangeMealBookingResponseDTO response = mealBookingService.bookRangeMeals(testUser, startDate, endDate);

        // Then
        assertTrue(response.getMessage().contains("successfully"));
        assertEquals(3, response.getBookedDates().size());
        verify(notificationService).createAndSendImmediately(
                testUser.getId(),
                "Meals booked",
                "Meals booked from " + startDate + " to " + endDate,
                NotificationType.BOOKING_CONFIRMATION
        );
    }

    @Test
    void shouldBookRangeMealsWithCancelledMealReactivation() {
        // Given
        LocalDate startDate = tomorrow;
        LocalDate endDate = tomorrow.plusDays(2);
        LocalDate middleDate = tomorrow.plusDays(1);

        // Mock cancelled booking for the middle date
        MealBooking cancelledBooking = MealBooking.builder()
                .id(1L)
                .user(testUser)
                .bookingDate(middleDate)
                .status(BookingStatus.CANCELLED)
                .build();

        when(mealBookingRepository.existsByUserAndBookingDateAndStatus(eq(testUser), any(LocalDate.class), eq(BookingStatus.BOOKED)))
                .thenReturn(false);
        when(mealBookingRepository.findByUserAndBookingDateAndStatus(testUser, middleDate, BookingStatus.CANCELLED))
                .thenReturn(Optional.of(cancelledBooking));
        when(mealBookingRepository.findByUserAndBookingDateAndStatus(eq(testUser), eq(startDate), eq(BookingStatus.CANCELLED)))
                .thenReturn(Optional.empty());
        when(mealBookingRepository.findByUserAndBookingDateAndStatus(eq(testUser), eq(endDate), eq(BookingStatus.CANCELLED)))
                .thenReturn(Optional.empty());
        when(mealBookingRepository.save(any(MealBooking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RangeMealBookingResponseDTO response = mealBookingService.bookRangeMeals(testUser, startDate, endDate);

        // Then
        assertTrue(response.getMessage().contains("successfully"));
        assertEquals(3, response.getBookedDates().size());
        assertTrue(response.getBookedDates().contains(startDate.toString()));
        assertTrue(response.getBookedDates().contains(middleDate.toString()));
        assertTrue(response.getBookedDates().contains(endDate.toString()));

        // Verify the cancelled booking was reactivated
        assertEquals(BookingStatus.BOOKED, cancelledBooking.getStatus());

        verify(notificationService).createAndSendImmediately(
                testUser.getId(),
                "Meals booked",
                "Meals booked from " + startDate + " to " + endDate,
                NotificationType.BOOKING_CONFIRMATION
        );
    }

    @Test
    void shouldBookRangeMealsWithMixedStatuses() {
        // Given
        LocalDate startDate = tomorrow;
        LocalDate endDate = tomorrow.plusDays(3); // 4 days total
        LocalDate secondDate = tomorrow.plusDays(1);
        LocalDate thirdDate = tomorrow.plusDays(2);
        LocalDate fourthDate = tomorrow.plusDays(3);

        // Mock: secondDate already BOOKED, thirdDate CANCELLED, others new
        MealBooking cancelledBooking = MealBooking.builder()
                .id(2L)
                .user(testUser)
                .bookingDate(thirdDate)
                .status(BookingStatus.CANCELLED)
                .build();

        when(mealBookingRepository.existsByUserAndBookingDateAndStatus(testUser, startDate, BookingStatus.BOOKED))
                .thenReturn(false);
        when(mealBookingRepository.existsByUserAndBookingDateAndStatus(testUser, secondDate, BookingStatus.BOOKED))
                .thenReturn(true); // Already booked - should be skipped
        when(mealBookingRepository.existsByUserAndBookingDateAndStatus(testUser, thirdDate, BookingStatus.BOOKED))
                .thenReturn(false);
        when(mealBookingRepository.existsByUserAndBookingDateAndStatus(testUser, fourthDate, BookingStatus.BOOKED))
                .thenReturn(false);

        when(mealBookingRepository.findByUserAndBookingDateAndStatus(testUser, startDate, BookingStatus.CANCELLED))
                .thenReturn(Optional.empty());
        when(mealBookingRepository.findByUserAndBookingDateAndStatus(testUser, thirdDate, BookingStatus.CANCELLED))
                .thenReturn(Optional.of(cancelledBooking));
        when(mealBookingRepository.findByUserAndBookingDateAndStatus(testUser, fourthDate, BookingStatus.CANCELLED))
                .thenReturn(Optional.empty());

        when(mealBookingRepository.save(any(MealBooking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RangeMealBookingResponseDTO response = mealBookingService.bookRangeMeals(testUser, startDate, endDate);

        // Then
        assertTrue(response.getMessage().contains("successfully"));
        assertEquals(3, response.getBookedDates().size()); // startDate, thirdDate (reactivated), fourthDate
        assertTrue(response.getBookedDates().contains(startDate.toString()));
        assertFalse(response.getBookedDates().contains(secondDate.toString())); // Already booked - skipped
        assertTrue(response.getBookedDates().contains(thirdDate.toString())); // Reactivated
        assertTrue(response.getBookedDates().contains(fourthDate.toString()));

        // Verify the cancelled booking was reactivated
        assertEquals(BookingStatus.BOOKED, cancelledBooking.getStatus());

        verify(notificationService).createAndSendImmediately(
                testUser.getId(),
                "Meals booked",
                "Meals booked from " + startDate + " to " + endDate,
                NotificationType.BOOKING_CONFIRMATION
        );
    }

    @Test
    void shouldGetUpcomingMealsSuccessfully() {
        // Given
        MealBooking pastBooking = MealBooking.builder()
                .id(1L)
                .user(testUser)
                .bookingDate(today.minusDays(1))
                .status(BookingStatus.BOOKED)
                .build();

        MealBooking todayBooking = MealBooking.builder()
                .id(2L)
                .user(testUser)
                .bookingDate(today)
                .status(BookingStatus.BOOKED)
                .build();

        MealBooking futureBooking = MealBooking.builder()
                .id(3L)
                .user(testUser)
                .bookingDate(tomorrow)
                .status(BookingStatus.BOOKED)
                .build();

        MealBooking cancelledBooking = MealBooking.builder()
                .id(4L)
                .user(testUser)
                .bookingDate(nextWeek)
                .status(BookingStatus.CANCELLED)
                .build();

        List<MealBooking> allBookings = Arrays.asList(pastBooking, todayBooking, futureBooking, cancelledBooking);
        when(mealBookingRepository.findByUserOrderByBookingDateDesc(testUser)).thenReturn(allBookings);

        // When
        UpcomingMealsResponseDTO response = mealBookingService.getUpcomingMeals(testUser);

        // Then
        assertNotNull(response);
        assertEquals(2, response.getBookedDates().size()); // Only today and future bookings with BOOKED status
        assertTrue(response.getBookedDates().contains(today));
        assertTrue(response.getBookedDates().contains(tomorrow));
        assertFalse(response.getBookedDates().contains(today.minusDays(1))); // Past date
        assertFalse(response.getBookedDates().contains(nextWeek)); // CANCELLED status
    }

    @Test
    void shouldCancelMealSuccessfully() {
        // Given
        CancelMealRequestDTO request = new CancelMealRequestDTO();
        request.setBookingDate(tomorrow);

        MealBooking existingBooking = MealBooking.builder()
                .id(123L)
                .user(testUser)
                .bookingDate(tomorrow)
                .status(BookingStatus.BOOKED)
                .build();

        when(mealBookingRepository.findByUserAndBookingDate(testUser, tomorrow)).thenReturn(Optional.of(existingBooking));

        // When
        SingleMealBookingResponseDTO response = mealBookingService.cancelMealByUserIdAndDate(testUser, request);

        // Then
        assertTrue(response.getMessage().contains("successfully"));
        assertEquals("Meal cancelled successfully for " + tomorrow, response.getMessage());
        assertEquals(tomorrow.toString(), response.getBookingDate());

        verify(mealBookingRepository).save(existingBooking);
        verify(notificationService).createAndSendImmediately(
                testUser.getId(),
                "Meal Cancelled",
                "Your meal booking for " + tomorrow + " has been cancelled successfully",
                NotificationType.CANCELLATION_CONFIRMATION
        );
    }

    @Test
    void shouldFailToCancelMealForPastDate() {
        // Given
        CancelMealRequestDTO request = new CancelMealRequestDTO();
        request.setBookingDate(today.minusDays(1));

        // When
        SingleMealBookingResponseDTO response = mealBookingService.cancelMealByUserIdAndDate(testUser, request);

        // Then
        assertFalse(response.getMessage().contains("successfully"));
        assertEquals("Cannot cancel meals for past dates", response.getMessage());
        verifyNoInteractions(mealBookingRepository);
        verifyNoInteractions(notificationRepository);
        verifyNoInteractions(pushNotificationService);
    }

    @Test
    void shouldFailToCancelMealAfterCutoffTime() {
        // Given - Set clock to after 10 PM for tomorrow's cancellation
        Clock lateNightClock = Clock.fixed(
                LocalDate.of(2026, 1, 25)
                        .atTime(22, 30) // 10:30 PM
                        .atZone(ZoneId.systemDefault())
                        .toInstant(),
                ZoneId.systemDefault()
        );
        ReflectionTestUtils.setField(mealBookingService, "clock", lateNightClock);

        CancelMealRequestDTO request = new CancelMealRequestDTO();
        request.setBookingDate(LocalDate.of(2026, 1, 26)); // Tomorrow

        // When
        SingleMealBookingResponseDTO response = mealBookingService.cancelMealByUserIdAndDate(testUser, request);

        // Then
        assertFalse(response.getMessage().contains("successfully"));
        assertEquals("Cancellation closed for tomorrow after 10 PM", response.getMessage());
        verifyNoInteractions(mealBookingRepository);
        verifyNoInteractions(notificationRepository);
        verifyNoInteractions(pushNotificationService);
    }

    @Test
    void shouldFailToCancelAlreadyCancelledMeal() {
        // Given
        CancelMealRequestDTO request = new CancelMealRequestDTO();
        request.setBookingDate(tomorrow);

        MealBooking cancelledBooking = MealBooking.builder()
                .id(123L)
                .user(testUser)
                .bookingDate(tomorrow)
                .status(BookingStatus.CANCELLED)
                .build();

        when(mealBookingRepository.findByUserAndBookingDate(testUser, tomorrow))
                .thenReturn(Optional.of(cancelledBooking));

        // When
        SingleMealBookingResponseDTO response = mealBookingService.cancelMealByUserIdAndDate(testUser, request);

        // Then
        assertFalse(response.getMessage().contains("successfully"));
        assertEquals("Cannot cancel meal that is not booked. Current status: CANCELLED", response.getMessage());
        verify(mealBookingRepository, never()).save(any());
        verifyNoInteractions(notificationRepository);
        verifyNoInteractions(pushNotificationService);
    }

    @Test
    void shouldFailToCancelMealWhenBookingNotFound() {
        // Given
        CancelMealRequestDTO request = new CancelMealRequestDTO();
        request.setBookingDate(tomorrow);

        when(mealBookingRepository.findByUserAndBookingDate(testUser, tomorrow)).thenReturn(Optional.empty());

        // When
        SingleMealBookingResponseDTO response = mealBookingService.cancelMealByUserIdAndDate(testUser, request);

        // Then
        assertFalse(response.getMessage().contains("successfully"));
        assertTrue(response.getMessage().contains("No booking found"));
        verifyNoInteractions(notificationRepository);
        verifyNoInteractions(pushNotificationService);
    }
}