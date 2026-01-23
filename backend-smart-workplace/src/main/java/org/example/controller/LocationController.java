package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.LocationUpdateRequestDTO;
import org.example.service.UserLocationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final UserLocationService locationService;

    @PostMapping("/update")
    public void updateLocation(
            @RequestBody LocationUpdateRequestDTO request,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        locationService.saveLocation(userId, request);
    }
}
