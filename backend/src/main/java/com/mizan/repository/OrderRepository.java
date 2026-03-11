package com.mizan.repository;

import com.mizan.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByPortfolioIdOrderByBuyDateAsc(UUID portfolioId);
    List<Order> findByUserIdOrderByBuyDateAsc(UUID userId);
    Optional<Order> findByIdAndUserId(UUID id, UUID userId);
    void deleteByIdAndUserId(UUID id, UUID userId);
}
