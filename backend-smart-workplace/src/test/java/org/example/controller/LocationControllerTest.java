package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.LocationUpdateRequestDTO;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.example.security.SecurityUserResolver;
import org.example.service.UserLocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.task.scheduling.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserLocationService userLocationService;

    @MockBean
    private SecurityUserResolver securityUserResolver;

    @MockBean
    private UserRepository userRepository;

    private LocationUpdateRequestDTO validLocationRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        validLocationRequest = LocationUpdateRequestDTO.builder()
                .latitude(18.5204)
                .longitude(73.8567)
                .build();

        mockUser = User.builder()
                .id(3L)
                .email("test@example.com")
                .name("Test User")
                .build();
    }

    @Test
    void updateLocation_ValidRequest_ShouldReturnSuccess() throws Exception {
        when(securityUserResolver.resolveUser()).thenReturn(mockUser);
        doNothing().when(userLocationService)
                .saveLocation(eq(3L), any(LocationUpdateRequestDTO.class));

        mockMvc.perform(post("/api/location/update")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLocationRequest)))
                .andExpect(status().isOk());

        verify(userLocationService).saveLocation(eq(3L), any());
    }

    @Test
    void updateLocation_MissingAuthentication_ShouldReturnUnauthorized() throws Exception {
        when(securityUserResolver.resolveUser()).thenThrow(new IllegalStateException("User not authenticated"));

        mockMvc.perform(post("/api/location/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLocationRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void updateLocation_EmptyBody_ShouldReturnBadRequest() throws Exception {
        when(securityUserResolver.resolveUser()).thenReturn(mockUser);

        mockMvc.perform(post("/api/location/update")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateLocation_InvalidLatitude_ShouldReturnBadRequest() throws Exception {
        when(securityUserResolver.resolveUser()).thenReturn(mockUser);

        LocationUpdateRequestDTO invalidRequest = LocationUpdateRequestDTO.builder()
                .latitude(91.0)
                .longitude(73.8567)
                .build();

        mockMvc.perform(post("/api/location/update")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateLocation_InvalidLongitude_ShouldReturnBadRequest() throws Exception {
        when(securityUserResolver.resolveUser()).thenReturn(mockUser);

        LocationUpdateRequestDTO invalidRequest = LocationUpdateRequestDTO.builder()
                .latitude(18.5204)
                .longitude(181.0)
                .build();

        mockMvc.perform(post("/api/location/update")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateLocation_BoundaryLatitudeValues_ShouldWork() throws Exception {
        when(securityUserResolver.resolveUser()).thenReturn(mockUser);

        LocationUpdateRequestDTO boundaryRequest = LocationUpdateRequestDTO.builder()
                .latitude(90.0)
                .longitude(180.0)
                .build();

        doNothing().when(userLocationService)
                .saveLocation(eq(3L), any());

        mockMvc.perform(post("/api/location/update")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(boundaryRequest)))
                .andExpect(status().isOk());

        verify(userLocationService).saveLocation(eq(3L), any());
    }

    @Test
    void updateLocation_NegativeBoundaryValues_ShouldWork() throws Exception {
        when(securityUserResolver.resolveUser()).thenReturn(mockUser);

        LocationUpdateRequestDTO boundaryRequest = LocationUpdateRequestDTO.builder()
                .latitude(-90.0)
                .longitude(-180.0)
                .build();

        doNothing().when(userLocationService)
                .saveLocation(eq(3L), any());

        mockMvc.perform(post("/api/location/update")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(boundaryRequest)))
                .andExpect(status().isOk());

        verify(userLocationService).saveLocation(eq(3L), any());
    }

    @Test
    void updateLocation_ServiceException_ShouldReturn500() throws Exception {
        when(securityUserResolver.resolveUser()).thenReturn(mockUser);

        doThrow(new RuntimeException("Service error"))
                .when(userLocationService)
                .saveLocation(eq(3L), any());

        mockMvc.perform(post("/api/location/update")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLocationRequest)))
                .andExpect(status().isInternalServerError());
    }
}
