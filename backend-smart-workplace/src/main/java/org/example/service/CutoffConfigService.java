package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.CutoffConfig;
import org.example.repository.CutoffConfigRepository;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class CutoffConfigService {

    private final CutoffConfigRepository cutoffConfigRepository;

    public CutoffConfig updateCutoffTime(LocalTime cutoffTime) {
        CutoffConfig config = CutoffConfig.builder()
                .cutoffTime(cutoffTime)
                .build();

        return cutoffConfigRepository.save(config);
    }

    public LocalTime getCurrentCutoffTime() {
        return cutoffConfigRepository.findTopByOrderByIdDesc()
                .map(CutoffConfig::getCutoffTime)
                .orElse(LocalTime.of(10, 0)); // default fallback
    }
}
