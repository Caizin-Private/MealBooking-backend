package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.MealBookingRequestDTO;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.example.service.MealBookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;



import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // ================= USER =================
    @Test
    @WithMockUser(
            username = "test.user@company.com",
            roles = {"USER"}
    )
    void shouldBookMealsSuccessfully() throws Exception {

        // GIVEN
        User mockUser = new User(
                1L,
                "Test User",
                "test.user@company.com",
                Role.USER,
                LocalDateTime.now()
        );

        when(userRepository.findByEmail("test.user@company.com"))
                .thenReturn(Optional.of(mockUser));

        doNothing().when(mealBookingService)
                .bookMeals(
                        any(User.class),
                        any(LocalDate.class),
                        any(LocalDate.class),
                        anyDouble(),
                        anyDouble()
                );

        MealBookingRequestDTO request = validRequest();

        // WHEN + THEN
        mockMvc.perform(
                        post("/api/meals/book")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(content().string("Meals booked successfully"));
    }

    // ================= ADMIN =================
    @Test
    @WithMockUser(username = "admin@company.com", roles = "ADMIN")
    void adminCanBookMeals() throws Exception {

        User admin = new User(
                2L,
                "Admin",
                "admin@company.com",
                Role.ADMIN,
                LocalDateTime.now()
        );

        when(userRepository.findByEmail("admin@company.com"))
                .thenReturn(Optional.of(admin));

        doNothing().when(mealBookingService)
                .bookMeals(
                        any(User.class),
                        any(LocalDate.class),
                        any(LocalDate.class),
                        anyDouble(),
                        anyDouble()
                );

        mockMvc.perform(
                        post("/api/meals/book")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isOk())
                .andExpect(content().string("Meals booked successfully"));
    }

    // ================= UNAUTH =================
    @Test
    void unauthenticatedUserCannotBookMeals() throws Exception {

        mockMvc.perform(
                        post("/api/meals/book")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isForbidden());
    }

    // ================= HELPER =================
    private MealBookingRequestDTO validRequest() {
        MealBookingRequestDTO request = new MealBookingRequestDTO();
        request.setStartDate(LocalDate.now().plusDays(2));
        request.setEndDate(LocalDate.now().plusDays(4));
        request.setLatitude(10.0);
        request.setLongitude(10.0);
        return request;
    }
}
