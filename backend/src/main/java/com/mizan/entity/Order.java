package com.mizan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(name = "stock_name")
    private String stockName;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "price_per_share", nullable = false, precision = 12, scale = 4)
    private BigDecimal pricePerShare;

    @Column(precision = 10, scale = 4)
    private BigDecimal commission;

    @Column(name = "total_cost", insertable = false, updatable = false, precision = 14, scale = 4)
    private BigDecimal totalCost;  // Computed column in DB

    @Column(name = "buy_date", nullable = false)
    private LocalDate buyDate;

    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
