package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.CutoffConfigRequest;
import org.example.entity.CutoffConfig;
import org.example.service.CutoffConfigService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cutoff")
@RequiredArgsConstructor
public class CutoffConfigController {

    private final CutoffConfigService cutoffConfigService;

    @PostMapping
    public CutoffConfig updateCutoff(@Valid @RequestBody CutoffConfigRequest request) {
        return cutoffConfigService.updateCutoffTime(request.getCutoffTime());
    }

    @GetMapping
    public String getCurrentCutoff() {
        return cutoffConfigService.getCurrentCutoffTime().toString();
    }
}
