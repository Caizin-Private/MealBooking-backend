package org.example.service;

import org.example.dto.SingleMealBookingResponseDTO;
import org.example.dto.RangeMealBookingResponseDTO;
import org.example.dto.UpcomingMealsResponseDTO;
import org.example.dto.CancelMealRequestDTO;
import org.example.entity.BookingStatus;
import org.example.entity.MealBooking;
import org.example.entity.User;
import org.example.repository.MealBookingRepository;
import org.example.repository.NotificationRepository;
import org.example.repository.UserRepository;
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
        when(mealBookingRepository.existsByUserAndBookingDate(testUser, tomorrow)).thenReturn(false);
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
        verify(pushNotificationService).sendSingleMealBookingConfirmation(testUser.getId(), tomorrow);
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
    void shouldFailToBookSingleMealForDuplicateBooking() {
        // Given
        when(mealBookingRepository.existsByUserAndBookingDate(testUser, tomorrow)).thenReturn(true);

        // When
        SingleMealBookingResponseDTO response = mealBookingService.bookSingleMeal(testUser, tomorrow);

        // Then
        assertFalse(response.getMessage().contains("successfully"));
        assertEquals("Meal already booked for " + tomorrow, response.getMessage());
        verifyNoInteractions(pushNotificationService);
    }

    @Test
    void shouldBookRangeMealsSuccessfully() {
        // Given
        LocalDate startDate = tomorrow;
        LocalDate endDate = tomorrow.plusDays(2);

        when(mealBookingRepository.existsByUserAndBookingDate(eq(testUser), any(LocalDate.class))).thenReturn(false);
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
        verify(pushNotificationService).sendBookingConfirmation(testUser.getId(), startDate, endDate);
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
        request.setUserId(1L);
        request.setBookingDate(tomorrow);

        MealBooking existingBooking = MealBooking.builder()
                .id(123L)
                .user(testUser)
                .bookingDate(tomorrow)
                .status(BookingStatus.BOOKED)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(mealBookingRepository.findByUserAndBookingDate(testUser, tomorrow)).thenReturn(Optional.of(existingBooking));

        // When
        SingleMealBookingResponseDTO response = mealBookingService.cancelMealByUserIdAndDate(request);

        // Then
        assertTrue(response.getMessage().contains("successfully"));
        assertEquals("Meal cancelled successfully for " + tomorrow, response.getMessage());
        assertEquals(tomorrow.toString(), response.getBookingDate());

        verify(mealBookingRepository).save(existingBooking);
        verify(notificationRepository).save(any());
        verify(pushNotificationService).sendCancellationConfirmation(testUser.getId(), tomorrow);
    }

    @Test
    void shouldFailToCancelMealForPastDate() {
        // Given
        CancelMealRequestDTO request = new CancelMealRequestDTO();
        request.setUserId(1L);
        request.setBookingDate(today.minusDays(1));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        SingleMealBookingResponseDTO response = mealBookingService.cancelMealByUserIdAndDate(request);

        // Then
        assertFalse(response.getMessage().contains("successfully"));
        assertEquals("Cannot cancel meals for past dates", response.getMessage());
        verifyNoInteractions(mealBookingRepository);
        verifyNoInteractions(notificationRepository);
        verifyNoInteractions(pushNotificationService);
    }

    @Test
    void shouldFailToCancelMealWhenUserNotFound() {
        // Given
        CancelMealRequestDTO request = new CancelMealRequestDTO();
        request.setUserId(999L);
        request.setBookingDate(tomorrow);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        SingleMealBookingResponseDTO response = mealBookingService.cancelMealByUserIdAndDate(request);

        // Then
        assertFalse(response.getMessage().contains("successfully"));
        assertTrue(response.getMessage().contains("User not found"));
        verifyNoInteractions(mealBookingRepository);
        verifyNoInteractions(notificationRepository);
        verifyNoInteractions(pushNotificationService);
    }

    @Test
    void shouldFailToCancelMealWhenBookingNotFound() {
        // Given
        CancelMealRequestDTO request = new CancelMealRequestDTO();
        request.setUserId(1L);
        request.setBookingDate(tomorrow);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(mealBookingRepository.findByUserAndBookingDate(testUser, tomorrow)).thenReturn(Optional.empty());

        // When
        SingleMealBookingResponseDTO response = mealBookingService.cancelMealByUserIdAndDate(request);

        // Then
        assertFalse(response.getMessage().contains("successfully"));
        assertTrue(response.getMessage().contains("No booking found"));
        verifyNoInteractions(notificationRepository);
        verifyNoInteractions(pushNotificationService);
    }
}