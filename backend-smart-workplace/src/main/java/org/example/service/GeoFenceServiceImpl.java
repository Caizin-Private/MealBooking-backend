package org.example.service;

import org.springframework.stereotype.Service;

@Service
public class GeoFenceServiceImpl implements GeoFenceService {

    @Override
    public boolean isInsideAllowedArea(double latitude, double longitude) {
        // TEMP implementation (for now)
        return true;
    }
}