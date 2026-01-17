package org.example.repository;

import org.example.entity.CutoffConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CutoffConfigRepository extends JpaRepository<CutoffConfig, Long> {
}