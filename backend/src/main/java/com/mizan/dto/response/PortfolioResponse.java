package com.mizan.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class PortfolioResponse {
    private UUID portfolioId;
    private String name;
    private BigDecimal totalInvested;
    private BigDecimal currentValue;
    private BigDecimal totalProfitLoss;
    private BigDecimal totalProfitLossPercent;
    private int positionCount;
    private List<PositionResponse> positions;

    @Data @Builder
    public static class PositionResponse {
        private String ticker;
        private String stockName;
        private int totalShares;
        private BigDecimal weightedAvgCost;
        private BigDecimal totalInvested;
        private BigDecimal currentPrice;
        private BigDecimal currentValue;
        private BigDecimal profitLoss;
        private BigDecimal profitLossPercent;
        private BigDecimal allocationPercent;
        private int orderCount;
        private List<OrderResponse> orders;
    }

    @Data @Builder
    public static class OrderResponse {
        private UUID id;
        private String ticker;
        private String stockName;
        private int quantity;
        private BigDecimal pricePerShare;
        private BigDecimal commission;
        private BigDecimal totalCost;
        private String buyDate;
        private String notes;
    }
}
