package com.mizan.service.impl;

import com.mizan.dto.request.CreateOrderRequest;
import com.mizan.dto.response.PortfolioResponse;
import com.mizan.dto.response.PortfolioResponse.OrderResponse;
import com.mizan.dto.response.PortfolioResponse.PositionResponse;
import com.mizan.entity.Order;
import com.mizan.entity.Portfolio;
import com.mizan.enums.AuditAction;
import com.mizan.repository.OrderRepository;
import com.mizan.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepo;
    private final OrderRepository orderRepo;
    private final AuditService auditService;

    @Cacheable(value = "portfolio-summary", key = "#userId")
    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolio(UUID userId) {
        Portfolio portfolio = portfolioRepo.findByUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new NoSuchElementException("Portfolio not found"));

        List<Order> orders = orderRepo.findByPortfolioIdOrderByBuyDateAsc(portfolio.getId());

        // Group by ticker
        Map<String, List<Order>> grouped = orders.stream()
                .collect(Collectors.groupingBy(Order::getTicker, LinkedHashMap::new, Collectors.toList()));

        BigDecimal totalInvested = BigDecimal.ZERO;
        List<PositionResponse> positions = new ArrayList<>();

        for (Map.Entry<String, List<Order>> entry : grouped.entrySet()) {
            String ticker = entry.getKey();
            List<Order> tickerOrders = entry.getValue();

            int totalShares = tickerOrders.stream().mapToInt(Order::getQuantity).sum();
            BigDecimal totalCost = tickerOrders.stream()
                    .map(o -> o.getPricePerShare().multiply(BigDecimal.valueOf(o.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal wac = totalShares > 0
                    ? totalCost.divide(BigDecimal.valueOf(totalShares), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            totalInvested = totalInvested.add(totalCost);

            List<OrderResponse> orderResponses = tickerOrders.stream()
                    .map(o -> OrderResponse.builder()
                            .id(o.getId())
                            .ticker(o.getTicker())
                            .stockName(o.getStockName())
                            .quantity(o.getQuantity())
                            .pricePerShare(o.getPricePerShare())
                            .commission(o.getCommission())
                            .totalCost(o.getTotalCost())
                            .buyDate(o.getBuyDate().toString())
                            .notes(o.getNotes())
                            .build())
                    .toList();

            positions.add(PositionResponse.builder()
                    .ticker(ticker)
                    .stockName(tickerOrders.get(0).getStockName())
                    .totalShares(totalShares)
                    .weightedAvgCost(wac)
                    .totalInvested(totalCost)
                    .orderCount(tickerOrders.size())
                    .orders(orderResponses)
                    // currentPrice, currentValue, profitLoss — populated by client with live prices
                    .build());
        }

        return PortfolioResponse.builder()
                .portfolioId(portfolio.getId())
                .name(portfolio.getName())
                .totalInvested(totalInvested)
                .positionCount(positions.size())
                .positions(positions)
                .build();
    }

    @CacheEvict(value = "portfolio-summary", key = "#userId")
    @Transactional
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {
        Portfolio portfolio = portfolioRepo.findByUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new NoSuchElementException("Portfolio not found"));

        Order order = Order.builder()
                .portfolio(portfolio)
                .userId(userId)
                .ticker(request.getTicker().toUpperCase())
                .stockName(request.getStockName())
                .quantity(request.getQuantity())
                .pricePerShare(request.getPricePerShare())
                .commission(request.getCommission())
                .buyDate(request.getBuyDate())
                .notes(request.getNotes())
                .build();

        order = orderRepo.save(order);

        auditService.logAsync(userId, AuditAction.ORDER_CREATED, null, null,
                Map.of("ticker", order.getTicker(), "quantity", order.getQuantity(),
                       "price", order.getPricePerShare().toString()));

        log.info("Order created: user={}, ticker={}, qty={}", userId, order.getTicker(), order.getQuantity());

        return OrderResponse.builder()
                .id(order.getId())
                .ticker(order.getTicker())
                .stockName(order.getStockName())
                .quantity(order.getQuantity())
                .pricePerShare(order.getPricePerShare())
                .commission(order.getCommission())
                .totalCost(order.getTotalCost())
                .buyDate(order.getBuyDate().toString())
                .notes(order.getNotes())
                .build();
    }

    @CacheEvict(value = "portfolio-summary", key = "#userId")
    @Transactional
    public void deleteOrder(UUID userId, UUID orderId) {
        Order order = orderRepo.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        orderRepo.delete(order);

        auditService.logAsync(userId, AuditAction.ORDER_DELETED, null, null,
                Map.of("orderId", orderId.toString(), "ticker", order.getTicker()));

        log.info("Order deleted: user={}, orderId={}", userId, orderId);
    }
}
