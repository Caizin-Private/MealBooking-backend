package org.example.repository;

import org.example.entity.CutoffConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CutoffConfigRepository extends JpaRepository<CutoffConfig, Long> {

    Optional<CutoffConfig> findTopByOrderByIdDesc();

}
