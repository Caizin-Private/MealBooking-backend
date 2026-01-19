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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;



import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
    @WithMockUser(username = "test.user@company.com", roles = "USER")
    void shouldBookMealsSuccessfully() throws Exception {

        when(userRepository.findByEmail("test.user@company.com"))
                .thenReturn(Optional.of(validUser()));

        doNothing().when(mealBookingService)
                .bookMeals(any(), any(), any(), anyDouble(), anyDouble());

        mockMvc.perform(
                        post("/api/meals/book")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isOk())
                .andExpect(content().string("Meals booked successfully"));
    }

    // ================= ADMIN =================
    @Test
    @WithMockUser(username = "admin@company.com", roles = "ADMIN")
    void adminCanBookMeals() throws Exception {

        when(userRepository.findByEmail("admin@company.com"))
                .thenReturn(new Optional[]{
                        Optional.of(
                                new User(
                                        2L,
                                        "Admin",
                                        "admin@company.com",
                                        Role.ADMIN,
                                        LocalDateTime.now()
                                )
                        )
                }[0]);

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
    void unauthenticatedUserCannotBookMeals() throws Exception {

        mockMvc.perform(
                        post("/api/meals/book")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isForbidden());
    }

    // ================= DUPLICATE BOOKING =================
    @Test
    @WithMockUser(username = "test.user@company.com", roles = "USER")
    void duplicateBookingThrowsException() {

        when(userRepository.findByEmail(any()))
                .thenReturn(Optional.of(validUser()));

        doThrow(new RuntimeException("Meal already booked"))
                .when(mealBookingService)
                .bookMeals(any(), any(), any(), anyDouble(), anyDouble());

        assertThrows(
                jakarta.servlet.ServletException.class,
                () -> mockMvc.perform(
                        post("/api/meals/book")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest()))
                )
        );
    }


    // ================= MEDIA TYPE =================
    @Test
    @WithMockUser(username = "ghost@company.com", roles = "USER")
    void userNotFoundThrowsException() {

        when(userRepository.findByEmail("ghost@company.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                jakarta.servlet.ServletException.class,
                () -> mockMvc.perform(
                        post("/api/meals/book")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest()))
                )
        );
    }

    @Test
    @WithMockUser(username = "test.user@company.com", roles = "USER")
    void cancelMealSuccessfully() throws Exception {

        User user = validUser();

        when(userRepository.findByEmail("test.user@company.com"))
                .thenReturn(Optional.of(user));

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
    @WithMockUser(username = "test.user@company.com", roles = "USER")
    void cancelFailsWhenNoBookingExists() throws Exception {

        User user = validUser();

        when(userRepository.findByEmail("test.user@company.com"))
                .thenReturn(Optional.of(user));

        doThrow(new RuntimeException("No booking found for this date"))
                .when(mealBookingService)
                .cancelMeal(any(User.class), any(LocalDate.class));

        Exception exception = assertThrows(Exception.class, () ->
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
        );

        assert exception.getCause() instanceof RuntimeException;
        assert exception.getCause().getMessage().equals("No booking found for this date");
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
                Role.USER,
                LocalDateTime.now()
        );
    }
}
