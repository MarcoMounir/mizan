package com.mizan.controller;

import com.mizan.dto.request.CreateOrderRequest;
import com.mizan.dto.response.PortfolioResponse;
import com.mizan.dto.response.PortfolioResponse.OrderResponse;
import com.mizan.service.impl.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping
    public ResponseEntity<PortfolioResponse> getPortfolio(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(portfolioService.getPortfolio(userId));
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = portfolioService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, String>> deleteOrder(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID orderId) {
        portfolioService.deleteOrder(userId, orderId);
        return ResponseEntity.ok(Map.of("message", "Order deleted"));
    }
}
