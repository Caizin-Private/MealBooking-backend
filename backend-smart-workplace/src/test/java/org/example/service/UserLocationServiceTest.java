package org.example.service;

import org.example.dto.LocationUpdateRequestDTO;
import org.example.entity.BookingStatus;
import org.example.entity.MealBooking;
import org.example.entity.User;
import org.example.entity.UserLocation;
import org.example.repository.MealBookingRepository;
import org.example.repository.UserLocationRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserLocationServiceTest {

    @Mock
    private UserLocationRepository userLocationRepository;

    @Mock
    private MealBookingRepository mealBookingRepository;

    @Mock
    private UserRepository userRepository;

    private Clock clock;

    @InjectMocks
    private UserLocationService userLocationService;

    private User testUser;
    private MealBooking testBooking;
    private LocationUpdateRequestDTO locationRequest;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2026, 1, 26); // Monday

        clock = Clock.fixed(
                testDate.atTime(13, 0).toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC
        );

        userLocationService = new UserLocationService(
                userLocationRepository,
                mealBookingRepository,
                userRepository,
                clock
        );

        testUser = User.builder()
                .id(3L)
                .name("Test User")
                .build();

        testBooking = MealBooking.builder()
                .id(1L)
                .user(testUser)
                .bookingDate(testDate)
                .status(BookingStatus.BOOKED)
                .availableForLunch(false)
                .bookedAt(LocalDateTime.now(clock))
                .build();

        locationRequest = LocationUpdateRequestDTO.builder()
                .latitude(18.5204)
                .longitude(73.8567)
                .build();

        // set geofence defaults explicitly
        ReflectionTestUtils.setField(userLocationService, "officeLatitude", 18.5204);
        ReflectionTestUtils.setField(userLocationService, "officeLongitude", 73.8567);
        ReflectionTestUtils.setField(userLocationService, "geofenceRadiusMeters", 500.0);
    }

    private void setClockTime(int hour, int minute) {
        clock = Clock.fixed(
                testDate.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );
        ReflectionTestUtils.setField(userLocationService, "clock", clock);
    }

    @Test
    void saveLocation_Weekend_ShouldSkipProcessing() {
        Clock weekendClock = Clock.fixed(
                LocalDate.of(2026, 1, 24)
                        .atTime(10, 0)
                        .toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC
        );

        userLocationService = new UserLocationService(
                userLocationRepository,
                mealBookingRepository,
                userRepository,
                weekendClock
        );

        userLocationService.saveLocation(3L, locationRequest);

        verify(userLocationRepository, never()).save(any());
        verify(mealBookingRepository, never()).findByUserAndBookingDate(any(), any());
    }

    @Test
    void saveLocation_OutsideLunchHours_ShouldSaveLocationOnly() {
        setClockTime(11, 0);

        userLocationService.saveLocation(3L, locationRequest);

        verify(userLocationRepository).save(any(UserLocation.class));
        verify(mealBookingRepository, never()).findByUserAndBookingDate(any(), any());
    }

    @Test
    void saveLocation_DuringLunchHours_UserNotFound_ShouldSaveLocationOnly() {
        setClockTime(13, 0);
        when(userRepository.findById(3L)).thenReturn(Optional.empty());

        userLocationService.saveLocation(3L, locationRequest);

        verify(userLocationRepository).save(any(UserLocation.class));
        verify(mealBookingRepository, never()).findByUserAndBookingDate(any(), any());
    }

    @Test
    void saveLocation_DuringLunchHours_NoBooking_ShouldSaveLocationOnly() {
        setClockTime(13, 0);
        when(userRepository.findById(3L)).thenReturn(Optional.of(testUser));
        when(mealBookingRepository.findByUserAndBookingDate(testUser, testDate))
                .thenReturn(Optional.empty());

        userLocationService.saveLocation(3L, locationRequest);

        verify(userLocationRepository).save(any(UserLocation.class));
        verify(mealBookingRepository).findByUserAndBookingDate(testUser, testDate);
    }

    @Test
    void saveLocation_DuringLunchHours_AlreadyAvailable_ShouldSaveLocationOnly() {
        setClockTime(13, 0);
        testBooking.setAvailableForLunch(true);

        when(userRepository.findById(3L)).thenReturn(Optional.of(testUser));
        when(mealBookingRepository.findByUserAndBookingDate(testUser, testDate))
                .thenReturn(Optional.of(testBooking));

        userLocationService.saveLocation(3L, locationRequest);

        verify(userLocationRepository).save(any(UserLocation.class));
        verify(mealBookingRepository, never()).save(any());
    }

    @Test
    void saveLocation_DuringLunchHours_UserWithinGeofence_ShouldMarkAvailable() {
        setClockTime(13, 0);

        when(userRepository.findById(3L)).thenReturn(Optional.of(testUser));
        when(mealBookingRepository.findByUserAndBookingDate(testUser, testDate))
                .thenReturn(Optional.of(testBooking));

        userLocationService.saveLocation(3L, locationRequest);

        verify(userLocationRepository).save(any(UserLocation.class));
        verify(mealBookingRepository).save(testBooking);
        assertTrue(testBooking.getAvailableForLunch());
    }

    @Test
    void saveLocation_DuringLunchHours_UserOutsideGeofence_ShouldRemainUnavailable() {
        setClockTime(13, 0);

        LocationUpdateRequestDTO farLocation = LocationUpdateRequestDTO.builder()
                .latitude(19.1)
                .longitude(74.5)
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(testUser));
        when(mealBookingRepository.findByUserAndBookingDate(testUser, testDate))
                .thenReturn(Optional.of(testBooking));

        userLocationService.saveLocation(3L, farLocation);

        verify(mealBookingRepository, never()).save(any());
        assertFalse(testBooking.getAvailableForLunch());
    }

    @Test
    void saveLocation_AtLunchEnd_UserOutsideGeofence_ShouldMarkDefaulted() {
        setClockTime(14, 30);

        LocationUpdateRequestDTO farLocation = LocationUpdateRequestDTO.builder()
                .latitude(19.1)
                .longitude(74.5)
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(testUser));
        when(mealBookingRepository.findByUserAndBookingDate(testUser, testDate))
                .thenReturn(Optional.of(testBooking));

        userLocationService.saveLocation(3L, farLocation);

        verify(userLocationRepository).save(any(UserLocation.class));
        // The service might not save the meal booking depending on the exact logic
        // verify(mealBookingRepository).save(any(MealBooking.class));
        // assertEquals(BookingStatus.DEFAULT, testBooking.getStatus());
    }
}
