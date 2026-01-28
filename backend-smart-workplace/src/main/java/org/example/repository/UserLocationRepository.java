package org.example.repository;

import org.example.entity.UserLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserLocationRepository extends JpaRepository<UserLocation, Long> {

    Optional<UserLocation> findTopByUserIdOrderByUpdatedAtDesc(Long userId);
}
