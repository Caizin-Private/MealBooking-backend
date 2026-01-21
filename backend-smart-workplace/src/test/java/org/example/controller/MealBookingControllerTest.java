package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.MealBookingRequestDTO;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.service.AuthenticatedUserService;
import org.example.service.MealBookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;



import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MealBookingController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class MealBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MealBookingService mealBookingService;

    @MockBean
    private AuthenticatedUserService authenticatedUserService;

    @Autowired
    private ObjectMapper objectMapper;

    // ================= USER =================
    @Test
    void shouldBookMealsSuccessfully() throws Exception {
        User user = validUser();
        when(authenticatedUserService.loadOrCreateUser(any()))
                .thenReturn(user);

        doNothing().when(mealBookingService)
                .bookMeals(any(), any(), any(), anyDouble(), anyDouble());

        mockMvc.perform(
                        post("/api/meals/book")
                                .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
                                        .jwt(jwt -> jwt.claim("email", "test.user@company.com").claim("roles", java.util.List.of("USER"))))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isOk())
                .andExpect(content().string("Meals booked successfully"));
    }

    // ================= ADMIN =================
    @Test
    void adminCanBookMeals() throws Exception {
        User admin = new User(
                2L,
                "Admin",
                "admin@company.com",
                null,
                null,
                null,
                Role.ADMIN,
                LocalDateTime.now(),
                null
        );

        when(authenticatedUserService.loadOrCreateUser(any()))
                .thenReturn(admin);

        doNothing().when(mealBookingService)
                .bookMeals(any(), any(), any(), anyDouble(), anyDouble());

        mockMvc.perform(
                        post("/api/meals/book")
                                .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
                                        .jwt(jwt -> jwt.claim("email", "admin@company.com").claim("roles", java.util.List.of("ADMIN"))))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isOk());
    }

    // ================= DUPLICATE BOOKING =================
    @Test
    void duplicateBookingThrowsException() throws Exception {
        User user = validUser();
        when(authenticatedUserService.loadOrCreateUser(any()))
                .thenReturn(user);

        doThrow(new RuntimeException("Meal already booked"))
                .when(mealBookingService)
                .bookMeals(any(), any(), any(), anyDouble(), anyDouble());

        mockMvc.perform(
                        post("/api/meals/book")
                                .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
                                        .jwt(jwt -> jwt.claim("email", "test.user@company.com").claim("roles", java.util.List.of("USER"))))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isInternalServerError());
    }


    // ================= USER CREATION =================
    @Test
    void newUserIsCreatedOnFirstLogin() throws Exception {
        User newUser = new User(
                1L,
                "New User",
                "newuser@company.com",
                "AZURE_AD",
                "oid-123",
                null,
                Role.USER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(authenticatedUserService.loadOrCreateUser(any()))
                .thenReturn(newUser);

        doNothing().when(mealBookingService)
                .bookMeals(any(), any(), any(), anyDouble(), anyDouble());

        mockMvc.perform(
                        post("/api/meals/book")
                                .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
                                        .jwt(jwt -> jwt.claim("email", "newuser@company.com").claim("roles", java.util.List.of("USER"))))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isOk());
    }

    @Test
    void cancelMealSuccessfully() throws Exception {
        User user = validUser();
        when(authenticatedUserService.loadOrCreateUser(any()))
                .thenReturn(user);

        doNothing().when(mealBookingService)
                .cancelMeal(any(User.class), any(LocalDate.class));

        mockMvc.perform(
                        delete("/api/meals/cancel")
                                .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
                                        .jwt(jwt -> jwt.claim("email", "test.user@company.com").claim("roles", java.util.List.of("USER"))))
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
        User user = validUser();
        when(authenticatedUserService.loadOrCreateUser(any()))
                .thenReturn(user);

        doThrow(new RuntimeException("No booking found for this date"))
                .when(mealBookingService)
                .cancelMeal(any(User.class), any(LocalDate.class));

        mockMvc.perform(
                        delete("/api/meals/cancel")
                                .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
                                        .jwt(jwt -> jwt.claim("email", "test.user@company.com").claim("roles", java.util.List.of("USER"))))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "date": "2026-01-22"
                                }
                            """)
                )
                .andExpect(status().isInternalServerError());
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
                "test.user@company.com",
                null,
                null,
                null,
                Role.USER,
                LocalDateTime.now(),
                null
        );
    }
}
