package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.LocationUpdateRequestDTO;
import org.example.entity.UserLocation;
import org.example.repository.UserLocationRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserLocationService {

    private final UserLocationRepository repository;
    private final Clock clock;

    public void saveLocation(Long userId, LocationUpdateRequestDTO request) {

        UserLocation location = UserLocation.builder()
                .userId(userId)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .updatedAt(LocalDateTime.now(clock))
                .build();

        repository.save(location);
    }
}
