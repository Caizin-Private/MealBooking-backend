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
import org.mockito.junit.jupiter.MockitoSettings;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserLocationServiceTest {

    @Mock
    private UserLocationRepository userLocationRepository;

    @Mock
    private MealBookingRepository mealBookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private UserLocationService userLocationService;

    private User testUser;
    private MealBooking testBooking;
    private LocationUpdateRequestDTO locationRequest;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2026, 1, 26);
        testUser = User.builder().id(3L).name("Test User").build();

        testBooking = MealBooking.builder()
                .id(1L)
                .user(testUser)
                .bookingDate(testDate)
                .status(BookingStatus.BOOKED)
                .availableForLunch(false)
                .bookedAt(LocalDateTime.now())
                .build();

        locationRequest = LocationUpdateRequestDTO.builder()
                .latitude(18.5204)
                .longitude(73.8567)
                .build();

        when(clock.instant()).thenReturn(
                testDate.atTime(13, 0).toInstant(ZoneOffset.UTC)
        );
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        // 🔥🔥🔥 THIS IS THE MISSING PART 🔥🔥🔥
        ReflectionTestUtils.setField(userLocationService, "officeLatitude", 18.5204);
        ReflectionTestUtils.setField(userLocationService, "officeLongitude", 73.8567);
        ReflectionTestUtils.setField(userLocationService, "geofenceRadiusMeters", 500.0);
    }
    private void lunchTime() {
        when(clock.instant())
                .thenReturn(testDate.atTime(13, 0).toInstant(ZoneOffset.UTC));
    }


    @Test
    void saveLocation_Weekend_ShouldSkipProcessing() {
        // Saturday
        LocalDate saturday = LocalDate.of(2026, 1, 24);
        when(clock.instant()).thenReturn(saturday.atStartOfDay().toInstant(java.time.ZoneOffset.UTC));

        userLocationService.saveLocation(3L, locationRequest);

        verify(userLocationRepository, never()).save(any());
        verify(mealBookingRepository, never()).findByUserAndBookingDate(any(), any());
    }

    @Test
    void saveLocation_OutsideLunchHours_ShouldSaveLocationOnly() {
        // 11:00 AM - before lunch hours
        when(clock.instant()).thenReturn(testDate.atTime(11, 0).toInstant(java.time.ZoneOffset.UTC));

        userLocationService.saveLocation(3L, locationRequest);

        verify(userLocationRepository).save(any(UserLocation.class));
        verify(mealBookingRepository, never()).findByUserAndBookingDate(any(), any());
    }

    @Test
    void saveLocation_DuringLunchHours_UserNotFound_ShouldSaveLocationOnly() {
        // 1:00 PM - during lunch hours
        when(clock.instant()).thenReturn(testDate.atTime(13, 0).toInstant(java.time.ZoneOffset.UTC));
        when(userRepository.findById(3L)).thenReturn(Optional.empty());

        userLocationService.saveLocation(3L, locationRequest);

        verify(userLocationRepository).save(any(UserLocation.class));
        verify(mealBookingRepository, never()).findByUserAndBookingDate(any(), any());
    }

    @Test
    void saveLocation_DuringLunchHours_NoBooking_ShouldSaveLocationOnly() {
        when(clock.instant()).thenReturn(testDate.atTime(13, 0).toInstant(java.time.ZoneOffset.UTC));
        when(userRepository.findById(3L)).thenReturn(Optional.of(testUser));
        when(mealBookingRepository.findByUserAndBookingDate(testUser, testDate))
                .thenReturn(Optional.empty());

        userLocationService.saveLocation(3L, locationRequest);

        verify(userLocationRepository).save(any(UserLocation.class));
        verify(mealBookingRepository).findByUserAndBookingDate(testUser, testDate);
    }

    @Test
    void saveLocation_DuringLunchHours_AlreadyAvailable_ShouldSaveLocationOnly() {
        testBooking.setAvailableForLunch(true);

        when(clock.instant()).thenReturn(testDate.atTime(13, 0).toInstant(java.time.ZoneOffset.UTC));
        when(userRepository.findById(3L)).thenReturn(Optional.of(testUser));
        when(mealBookingRepository.findByUserAndBookingDate(testUser, testDate))
                .thenReturn(Optional.of(testBooking));

        userLocationService.saveLocation(3L, locationRequest);

        verify(userLocationRepository).save(any(UserLocation.class));
        verify(mealBookingRepository, never()).save(any());
    }

    @Test
    void saveLocation_DuringLunchHours_UserWithinGeofence_ShouldMarkAvailable() {
        lunchTime();

        when(userRepository.findById(3L)).thenReturn(Optional.of(testUser));
        when(mealBookingRepository.findByUserAndBookingDate(testUser, testDate))
                .thenReturn(Optional.of(testBooking));

        LocationUpdateRequestDTO location = LocationUpdateRequestDTO.builder()
                .latitude(18.5204)
                .longitude(73.8567)
                .build();

        userLocationService.saveLocation(3L, location);

        verify(userLocationRepository).save(any(UserLocation.class));
        verify(mealBookingRepository).save(testBooking);
        assertTrue(testBooking.getAvailableForLunch());
    }


    @Test
    void saveLocation_DuringLunchHours_UserOutsideGeofence_ShouldRemainUnavailable() {

        LocationUpdateRequestDTO farLocation = LocationUpdateRequestDTO.builder()
                .latitude(19.1)
                .longitude(74.5)
                .build();

        when(clock.instant()).thenReturn(testDate.atTime(13, 0).toInstant(ZoneOffset.UTC));
        when(userRepository.findById(3L)).thenReturn(Optional.of(testUser));
        when(mealBookingRepository.findByUserAndBookingDate(testUser, testDate))
                .thenReturn(Optional.of(testBooking));

        userLocationService.saveLocation(3L, farLocation);

        verify(mealBookingRepository, never()).save(any());
        assertFalse(testBooking.getAvailableForLunch());
    }


    @Test
    void saveLocation_AtLunchEnd_UserOutsideGeofence_ShouldMarkDefaulted() {
        when(clock.instant())
                .thenReturn(testDate.atTime(14, 30).toInstant(ZoneOffset.UTC));

        when(userRepository.findById(3L)).thenReturn(Optional.of(testUser));
        when(mealBookingRepository.findByUserAndBookingDate(testUser, testDate))
                .thenReturn(Optional.of(testBooking));

        LocationUpdateRequestDTO far = LocationUpdateRequestDTO.builder()
                .latitude(19.1)
                .longitude(74.5)
                .build();

        userLocationService.saveLocation(3L, far);

        verify(userLocationRepository).save(any(UserLocation.class));
        verify(mealBookingRepository).save(testBooking);
        assertEquals(BookingStatus.DEFAULT, testBooking.getStatus());
    }

    @Test
    void isUserWithinGeofence_OfficeLocation_ShouldReturnTrue() {

        LocationUpdateRequestDTO officeLocation = LocationUpdateRequestDTO.builder()
                .latitude(18.5204)
                .longitude(73.8567)
                .build();

        when(clock.instant()).thenReturn(testDate.atTime(13, 0).toInstant(ZoneOffset.UTC));
        when(userRepository.findById(3L)).thenReturn(Optional.of(testUser));
        when(mealBookingRepository.findByUserAndBookingDate(testUser, testDate))
                .thenReturn(Optional.of(testBooking));

        userLocationService.saveLocation(3L, officeLocation);

        assertTrue(testBooking.getAvailableForLunch());
    }


    @Test
    void isUserWithinGeofence_NearbyLocation_ShouldReturnTrue() {
        lunchTime();

        when(userRepository.findById(3L)).thenReturn(Optional.of(testUser));
        when(mealBookingRepository.findByUserAndBookingDate(testUser, testDate))
                .thenReturn(Optional.of(testBooking));

        LocationUpdateRequestDTO nearby = LocationUpdateRequestDTO.builder()
                .latitude(18.5206)
                .longitude(73.8569)
                .build();

        userLocationService.saveLocation(3L, nearby);

        verify(userLocationRepository).save(any(UserLocation.class));
        assertTrue(testBooking.getAvailableForLunch());
    }


    @Test
    void isUserWithinGeofence_FarLocation_ShouldReturnFalse() {

        LocationUpdateRequestDTO farLocation = LocationUpdateRequestDTO.builder()
                .latitude(19.1)
                .longitude(74.5)
                .build();

        when(clock.instant()).thenReturn(testDate.atTime(13, 0).toInstant(ZoneOffset.UTC));
        when(userRepository.findById(3L)).thenReturn(Optional.of(testUser));
        when(mealBookingRepository.findByUserAndBookingDate(testUser, testDate))
                .thenReturn(Optional.of(testBooking));

        userLocationService.saveLocation(3L, farLocation);

        assertFalse(testBooking.getAvailableForLunch());
    }

}
