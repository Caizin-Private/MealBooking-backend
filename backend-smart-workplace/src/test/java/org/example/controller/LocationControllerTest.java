package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.LocationUpdateRequestDTO;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserLocationService userLocationService;

    private LocationUpdateRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        validRequest = LocationUpdateRequestDTO.builder()
                .latitude(18.5204)
                .longitude(73.8567)
                .build();
    }

    @Test
    void updateLocation_ValidRequest_ShouldReturnSuccess() throws Exception {

        mockMvc.perform(post("/api/location/update")
                        .header("X-USER-ID", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        verify(userLocationService).saveLocation(eq(3L), any());
    }

    @Test
    void updateLocation_MissingUserIdHeader_ShouldReturnBadRequest() throws Exception {

        mockMvc.perform(post("/api/location/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userLocationService);
    }

    @Test
    void updateLocation_EmptyBody_ShouldReturnBadRequest() throws Exception {

        mockMvc.perform(post("/api/location/update")
                        .header("X-USER-ID", "3")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userLocationService);
    }

    @Test
    void updateLocation_InvalidLatitude_ShouldStillCallService() throws Exception {

        LocationUpdateRequestDTO invalid = LocationUpdateRequestDTO.builder()
                .latitude(91.0)
                .longitude(73.0)
                .build();

        mockMvc.perform(post("/api/location/update")
                        .header("X-USER-ID", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isOk());

        verify(userLocationService).saveLocation(eq(3L), any());
    }

    @Test
    void updateLocation_InvalidLongitude_ShouldStillCallService() throws Exception {

        LocationUpdateRequestDTO invalid = LocationUpdateRequestDTO.builder()
                .latitude(18.0)
                .longitude(181.0)
                .build();

        mockMvc.perform(post("/api/location/update")
                        .header("X-USER-ID", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isOk());

        verify(userLocationService).saveLocation(eq(3L), any());
    }

    @Test
    void updateLocation_ServiceException_ShouldReturn500() throws Exception {

        doThrow(new RuntimeException("boom"))
                .when(userLocationService).saveLocation(any(), any());

        mockMvc.perform(post("/api/location/update")
                        .header("X-USER-ID", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError());
    }
}
