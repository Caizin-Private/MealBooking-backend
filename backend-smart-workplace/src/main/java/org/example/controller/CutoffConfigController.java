package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.service.CutoffConfigService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cutoff")
@RequiredArgsConstructor
@Tag(name = "Cutoff Configuration", description = "APIs for getting meal booking cutoff times")
@SecurityRequirement(name = "bearerAuth")
public class CutoffConfigController {

    private final CutoffConfigService cutoffConfigService;

    @GetMapping
    @Operation(
            summary = "Get current cutoff time",
            description = "Get the current cutoff time for meal bookings. After this time, users cannot book meals for the current day."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Cutoff time retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Map.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing token"
            )
    })
    public Map<String, Object> getCurrentCutoffTime() {
        LocalTime cutoffTime = cutoffConfigService.getCurrentCutoffTime();
        boolean isAfterCutoff = cutoffConfigService.isAfterCutoffTime();

        Map<String, Object> response = new HashMap<>();
        response.put("cutoffTime", cutoffTime.toString());
        response.put("isAfterCutoff", isAfterCutoff);
        response.put("message", isAfterCutoff ?
                "Current time is after cutoff. Meal booking is closed for today." :
                "Meal booking is currently open.");

        return response;
    }
}
