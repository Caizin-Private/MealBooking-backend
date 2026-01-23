package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.CutoffConfigRequest;
import org.example.entity.CutoffConfig;
import org.example.service.CutoffConfigService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cutoff")
@RequiredArgsConstructor
@Tag(name = "Cutoff Configuration", description = "APIs for managing meal booking cutoff times")
@SecurityRequirement(name = "basicAuth")
public class CutoffConfigController {

    private final CutoffConfigService cutoffConfigService;

    @PostMapping
    @Operation(
            summary = "Update cutoff time",
            description = "Update the global cutoff time for meal bookings. After this time, users cannot book meals for the current day."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Cutoff time updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CutoffConfig.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid time format",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = "Invalid time format")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid credentials"
            )
    })
    public CutoffConfig updateCutoff(
            @Parameter(
                    description = "Cutoff time configuration",
                    required = true,
                    schema = @Schema(implementation = CutoffConfigRequest.class)
            )
            @Valid @RequestBody CutoffConfigRequest request
    ) {
        return cutoffConfigService.updateCutoffTime(request.getCutoffTime());
    }

    @GetMapping
    @Operation(
            summary = "Get current cutoff time",
            description = "Retrieve the current global cutoff time for meal bookings. Returns time in HH:mm format."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Current cutoff time retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "22:00")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid credentials"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No cutoff configuration found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = "No cutoff configuration found")
                    )
            )
    })
    public String getCurrentCutoff() {
        return cutoffConfigService.getCurrentCutoffTime().toString();
    }
}
