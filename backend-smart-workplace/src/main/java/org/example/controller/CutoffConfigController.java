package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.CutoffConfigRequest;
import org.example.entity.CutoffConfig;
import org.example.service.CutoffConfigService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cutoff")
@RequiredArgsConstructor
public class CutoffConfigController {

    private final CutoffConfigService cutoffConfigService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public CutoffConfig updateCutoff(@Valid @RequestBody CutoffConfigRequest request) {
        return cutoffConfigService.updateCutoffTime(request.getCutoffTime());
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping
    public String getCurrentCutoff() {
        return cutoffConfigService.getCurrentCutoffTime().toString();
    }
}
