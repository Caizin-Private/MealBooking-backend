package org.example.repository;

import org.example.entity.UserLocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLocationRepository
        extends JpaRepository<UserLocation, Long> {
}
