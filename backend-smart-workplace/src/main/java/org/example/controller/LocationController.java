package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.LocationUpdateRequestDTO;
import org.example.entity.User;
import org.example.service.AuthenticatedUserService;
import org.example.service.UserLocationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final UserLocationService locationService;
    private final AuthenticatedUserService authenticatedUserService;

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping("/update")
    public void updateLocation(
            @RequestBody LocationUpdateRequestDTO request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        User user = authenticatedUserService.loadOrCreateUser(jwt);
        locationService.saveLocation(user.getId(), request);
    }
}
