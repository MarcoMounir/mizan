package com.mizan.controller;

import com.mizan.entity.User;
import com.mizan.repository.UserRepository;
import com.mizan.service.impl.MarketDataService;
import com.mizan.service.impl.MarketDataService.SearchResult;
import com.mizan.service.impl.MarketDataService.StockQuote;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/market")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataService marketDataService;
    private final UserRepository userRepository;

    @GetMapping("/quotes")
    public ResponseEntity<Map<String, StockQuote>> getQuotes(
            @AuthenticationPrincipal UUID userId,
            @RequestParam String tickers) {
        String apiKey = getUserApiKey(userId);
        List<String> list = Arrays.asList(tickers.toUpperCase().split(","));
        return ResponseEntity.ok(marketDataService.getQuotes(list, apiKey));
    }

    @GetMapping("/search")
    public ResponseEntity<List<SearchResult>> search(
            @AuthenticationPrincipal UUID userId,
            @RequestParam String q) {
        String apiKey = getUserApiKey(userId);
        return ResponseEntity.ok(marketDataService.searchSymbols(q, apiKey));
    }

    @PostMapping("/validate-key")
    public ResponseEntity<Map<String, Object>> validateKey(@RequestBody Map<String, String> body) {
        StockQuote quote = marketDataService.validateApiKey(body.get("apiKey"));
        if (quote != null) {
            return ResponseEntity.ok(Map.of("valid", true,
                    "message", "Connected — COMI: " + quote.getClose() + " EGP", "testQuote", quote));
        }
        return ResponseEntity.ok(Map.of("valid", false, "message", "Invalid API key or unavailable"));
    }

    private String getUserApiKey(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getTwelveDataApiKeyEncrypted)
                .orElse(null);
        // TODO: decrypt with AES-256 before returning
    }
}
