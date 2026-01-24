package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.MealBookingRequestDTO;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.example.service.MealBookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(org.example.config.TestSecurityConfig.class)
@ActiveProfiles("test")
class MealBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MealBookingService mealBookingService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testEndpointWorks() throws Exception {
        mockMvc.perform(
                        get("/api/meals/test")
                )
                .andDo(result -> System.out.println("Test endpoint - Response status: " + result.getResponse().getStatus()))
                .andDo(result -> System.out.println("Test endpoint - Response content: " + result.getResponse().getContentAsString()))
                .andDo(result -> System.out.println("Test endpoint - Handler: " + result.getHandler()))
                .andExpect(status().isOk())
                .andExpect(content().string("Controller is working"));
    }

    // ================= USER =================

    @Test
    void shouldBookMealsSuccessfully() throws Exception {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(validUser()));

        doNothing().when(mealBookingService)
                .bookMeals(any(), any(), any(), anyDouble(), anyDouble());

        mockMvc.perform(
                        post("/api/meals/book")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andDo(result -> System.out.println("Response status: " + result.getResponse().getStatus()))
                .andDo(result -> System.out.println("Response content: " + result.getResponse().getContentAsString()))
                .andDo(result -> System.out.println("Handler: " + result.getHandler()))
                .andDo(result -> System.out.println("Exception: " + result.getResolvedException()))
                .andExpect(status().isOk());

        verify(mealBookingService, times(1))
                .bookMeals(any(), any(), any(), anyDouble(), anyDouble());
    }

    // ================= ADMIN =================

    @Test
    void adminCanBookMeals() throws Exception {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(
                        new User(
                                2L,
                                "Admin",
                                "test@example.com",
                                Role.ADMIN,
                                LocalDateTime.now()
                        )
                ));

        doNothing().when(mealBookingService)
                .bookMeals(any(), any(), any(), anyDouble(), anyDouble());

        mockMvc.perform(
                        post("/api/meals/book")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isOk());
    }

    // ================= SECURITY =================

    @Test
    void unauthenticatedUserCanBookMeals() throws Exception {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(validUser()));

        doNothing().when(mealBookingService)
                .bookMeals(any(), any(), any(), anyDouble(), anyDouble());

        mockMvc.perform(
                        post("/api/meals/book")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isOk());
    }

    @Test
    void csrfMissingReturns200() throws Exception {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(validUser()));

        doNothing().when(mealBookingService)
                .bookMeals(any(), any(), any(), anyDouble(), anyDouble());

        mockMvc.perform(
                        post("/api/meals/book")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isOk());
    }

    // ================= VALIDATION =================

    @Test
    void invalidRequestReturns400() throws Exception {

        mockMvc.perform(
                        post("/api/meals/book")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest());
    }

    // ================= EXCEPTION CASES =================

    @Test
    void duplicateBookingReturns400() throws Exception {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(validUser()));

        doThrow(new RuntimeException("Meal already booked"))
                .when(mealBookingService)
                .bookMeals(any(), any(), any(), anyDouble(), anyDouble());

        mockMvc.perform(
                        post("/api/meals/book")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Meal already booked"));
    }

    @Test
    void userNotFoundReturns400() throws Exception {

        // Mock repository to throw exception when trying to save
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(
                        post("/api/meals/book")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isBadRequest());
    }

    // ================= CANCEL MEAL =================

    @Test
    void cancelMealSuccessfully() throws Exception {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(validUser()));

        doNothing().when(mealBookingService)
                .cancelMeal(any(User.class), any(LocalDate.class));

        mockMvc.perform(
                        delete("/api/meals/cancel")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "date": "2026-01-22"
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("Meal booking cancelled successfully"));
    }

    @Test
    void cancelFailsWhenNoBookingExists() throws Exception {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(validUser()));

        doThrow(new RuntimeException("No booking found for this date"))
                .when(mealBookingService)
                .cancelMeal(any(User.class), any(LocalDate.class));

        mockMvc.perform(
                        delete("/api/meals/cancel")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "date": "2026-01-22"
                                }
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No booking found for this date"));
    }

    // ================= HELPERS =================

    private MealBookingRequestDTO validRequest() {
        MealBookingRequestDTO request = new MealBookingRequestDTO();
        request.setStartDate(LocalDate.now().plusDays(2));
        request.setEndDate(LocalDate.now().plusDays(4));
        request.setLatitude(10.0);
        request.setLongitude(10.0);
        return request;
    }

    private User validUser() {
        return new User(
                1L,
                "Test User",
                "test@example.com",
                Role.USER,
                LocalDateTime.now()
        );
    }
}
