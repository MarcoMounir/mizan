package com.mizan.repository;

import com.mizan.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {
    Optional<Portfolio> findByUserIdAndIsDefaultTrue(UUID userId);
}
