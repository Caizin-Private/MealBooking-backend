package org.example.service;

public interface GeoFenceService {
    boolean isInsideAllowedArea(double latitude, double longitude);
}