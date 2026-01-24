package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.SingleMealBookingRequestDTO;
import org.example.dto.RangeMealBookingRequestDTO;
import org.example.dto.UpcomingMealsRequestDTO;
import org.example.dto.CancelMealRequestDTO;
import org.example.dto.MealBookingResponseDTO;
import org.example.dto.RangeMealBookingResponseDTO;
import org.example.dto.UpcomingMealsResponseDTO;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.example.service.MealBookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

    private User testUser;

    @Test
    @WithMockUser(roles = "USER")
    void shouldBookSingleMealSuccessfully() throws Exception {
        // Setup
        testUser = new User(1L, "Test User", "test@example.com", Role.USER, LocalDateTime.now());

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        SingleMealBookingRequestDTO request = new SingleMealBookingRequestDTO();
        request.setDate(LocalDate.now().plusDays(1));

        MealBookingResponseDTO response = MealBookingResponseDTO.success(
                "Meal booked successfully for " + request.getDate(),
                123L,
                request.getDate().toString()
        );

        when(mealBookingService.bookSingleMeal(any(User.class), any(LocalDate.class)))
                .thenReturn(response);

        // Test
        mockMvc.perform(post("/api/meals/book-single")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.bookingId").value(123))
                .andExpect(jsonPath("$.bookingDate").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldBookRangeMealsSuccessfully() throws Exception {
        // Setup
        testUser = new User(1L, "Test User", "test@example.com", Role.USER, LocalDateTime.now());

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        RangeMealBookingRequestDTO request = new RangeMealBookingRequestDTO();
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));

        RangeMealBookingResponseDTO response = RangeMealBookingResponseDTO.success(
                "All meals booked successfully",
                List.of("2026-01-26", "2026-01-27", "2026-01-28")
        );

        when(mealBookingService.bookRangeMeals(any(User.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(response);

        // Test
        mockMvc.perform(post("/api/meals/book-range")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.bookedDates").isArray());
    }

    @Test
    void shouldGetUpcomingMealsSuccessfully() throws Exception {
        // Setup
        testUser = new User(1L, "Test User", "test@example.com", Role.USER, LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UpcomingMealsRequestDTO request = new UpcomingMealsRequestDTO();
        request.setUserId(1L);

        UpcomingMealsResponseDTO response = new UpcomingMealsResponseDTO(
                List.of(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2))
        );

        when(mealBookingService.getUpcomingMeals(any(User.class)))
                .thenReturn(response);

        // Test
        mockMvc.perform(post("/api/meals/upcoming")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookedDates").isArray())
                .andExpect(jsonPath("$.bookedDates[0]").exists());
    }

    @Test
    void shouldCancelMealSuccessfully() throws Exception {
        // Setup
        testUser = new User(1L, "Test User", "test@example.com", Role.USER, LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        CancelMealRequestDTO request = new CancelMealRequestDTO();
        request.setUserId(1L);
        request.setBookingDate(LocalDate.now().plusDays(1));

        MealBookingResponseDTO response = MealBookingResponseDTO.success(
                "Meal cancelled successfully for " + request.getBookingDate(),
                123L,
                request.getBookingDate().toString()
        );

        when(mealBookingService.cancelMealByUserIdAndDate(any(CancelMealRequestDTO.class)))
                .thenReturn(response);

        // Test
        mockMvc.perform(post("/api/meals/cancel")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.bookingId").value(123))
                .andExpect(jsonPath("$.bookingDate").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnBadRequestForInvalidSingleMealRequest() throws Exception {
        // Test with missing date
        SingleMealBookingRequestDTO request = new SingleMealBookingRequestDTO();

        mockMvc.perform(post("/api/meals/book-single")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnBadRequestForInvalidRangeMealRequest() throws Exception {
        // Test with missing dates
        RangeMealBookingRequestDTO request = new RangeMealBookingRequestDTO();

        mockMvc.perform(post("/api/meals/book-range")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForInvalidUpcomingMealsRequest() throws Exception {
        // Test with missing userId
        UpcomingMealsRequestDTO request = new UpcomingMealsRequestDTO();

        mockMvc.perform(post("/api/meals/upcoming")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForInvalidCancelRequest() throws Exception {
        // Test with missing fields
        CancelMealRequestDTO request = new CancelMealRequestDTO();

        mockMvc.perform(post("/api/meals/cancel")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldHandleUserNotFound() throws Exception {
        // Setup
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        SingleMealBookingRequestDTO request = new SingleMealBookingRequestDTO();
        request.setDate(LocalDate.now().plusDays(1));

        // Test
        mockMvc.perform(post("/api/meals/book-single")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());
    }
}