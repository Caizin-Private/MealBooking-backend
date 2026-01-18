package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.MealBookingRequestDTO;
import org.example.entity.User;
import org.example.service.MealBookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;



import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MealBookingController.class)
class MealBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MealBookingService mealBookingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(
            username = "test.user@company.com",
            roles = {"USER"}
    )
    void shouldBookMealsSuccessfully() throws Exception {

        // GIVEN
        MealBookingRequestDTO request = new MealBookingRequestDTO();
        request.setBookingDates(List.of(LocalDate.now().plusDays(2)));
        request.setLatitude(10.0);
        request.setLongitude(10.0);

        doNothing().when(mealBookingService)
                .bookMeals(
                        any(User.class),
                        anyList(),
                        anyDouble(),
                        anyDouble()
                );

        // WHEN + THEN
        mockMvc.perform(
                        post("/api/meals/book")
                                .with(csrf())   // ✅ THIS WAS MISSING
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Meals booked successfully"))
                .andExpect(jsonPath("$.dates").isArray());
    }

    @Test
    void unauthenticatedUserCannotBookMeals() throws Exception {

        MealBookingRequestDTO request = new MealBookingRequestDTO();
        request.setBookingDates(List.of(LocalDate.now().plusDays(2)));
        request.setLatitude(10.0);
        request.setLongitude(10.0);

        mockMvc.perform(
                        post("/api/meals/book")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());
    }

}
