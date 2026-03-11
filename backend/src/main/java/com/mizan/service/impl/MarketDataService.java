package com.mizan.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.io.Serializable;
import java.time.Duration;
import java.util.*;

/**
 * Twelve Data market data service for EGX stocks (BYOK architecture).
 *
 * Each user stores their own free Twelve Data API key (encrypted in DB).
 * Backend calls Twelve Data on user's behalf, caches results in Redis.
 * Cache is shared — prices are public data. One fetch serves all users.
 *
 * Twelve Data free tier: 8 calls/min, 800 calls/day per key.
 * With 3-min Redis cache, a user checking 10 stocks uses ~2 calls per session.
 *
 * Endpoints:
 *   GET /quote?symbol=COMI,ETEL&exchange=EGX&apikey=KEY
 *   GET /symbol_search?symbol=query&exchange=EGX&apikey=KEY
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataService {

    private final StringRedisTemplate redisTemplate;
    private final Gson gson = new Gson();

    @Value("${mizan.market-data.cache-ttl-seconds:180}")
    private long cacheTtlSeconds;

    private static final String TD_BASE = "https://api.twelvedata.com";
    private static final String CACHE_PREFIX = "price:";
    private static final String SEARCH_CACHE_PREFIX = "search:";
    private static final int BATCH_SIZE = 8;

    private WebClient webClient;

    @PostConstruct
    void init() {
        this.webClient = WebClient.builder()
                .baseUrl(TD_BASE)
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(512 * 1024))
                .build();
        log.info("Twelve Data MarketDataService initialized");
    }

    // ── BATCH QUOTES ─────────────────────────────────────────

    public Map<String, StockQuote> getQuotes(List<String> tickers, String apiKey) {
        if (tickers == null || tickers.isEmpty()) return Collections.emptyMap();

        Map<String, StockQuote> result = new LinkedHashMap<>();
        List<String> misses = new ArrayList<>();

        // Check Redis cache first
        for (String t : tickers) {
            String key = t.toUpperCase();
            String cached = safeGet(CACHE_PREFIX + key);
            if (cached != null) {
                try { result.put(key, gson.fromJson(cached, StockQuote.class)); continue; }
                catch (Exception e) { /* fall through */ }
            }
            misses.add(key);
        }

        if (misses.isEmpty() || apiKey == null || apiKey.isBlank()) return result;

        // Fetch in batches of 8
        for (int i = 0; i < misses.size(); i += BATCH_SIZE) {
            List<String> batch = misses.subList(i, Math.min(i + BATCH_SIZE, misses.size()));
            Map<String, StockQuote> fetched = fetchBatch(batch, apiKey);
            fetched.forEach((ticker, quote) -> {
                result.put(ticker, quote);
                safeSet(CACHE_PREFIX + ticker, gson.toJson(quote), Duration.ofSeconds(cacheTtlSeconds));
            });
        }

        log.info("Quotes: {} total, {} cached, {} fetched", result.size(), tickers.size() - misses.size(), misses.size());
        return result;
    }

    private Map<String, StockQuote> fetchBatch(List<String> tickers, String apiKey) {
        Map<String, StockQuote> result = new LinkedHashMap<>();
        String symbols = String.join(",", tickers);

        try {
            String body = webClient.get()
                    .uri(u -> u.path("/quote")
                            .queryParam("symbol", symbols)
                            .queryParam("exchange", "EGX")
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(15));

            if (body == null || body.isBlank()) return result;
            JsonObject json = gson.fromJson(body, JsonObject.class);

            if (tickers.size() == 1) {
                StockQuote q = parseQuote(tickers.get(0), json);
                if (q != null) result.put(tickers.get(0), q);
            } else {
                for (String t : tickers) {
                    JsonElement el = json.get(t);
                    if (el != null && el.isJsonObject()) {
                        StockQuote q = parseQuote(t, el.getAsJsonObject());
                        if (q != null) result.put(t, q);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Twelve Data fetch failed [{}]: {}", symbols, e.getMessage());
        }
        return result;
    }

    private StockQuote parseQuote(String ticker, JsonObject j) {
        if (j.has("code") || ("error".equals(str(j, "status")))) {
            log.warn("API error for {}: {}", ticker, str(j, "message"));
            return null;
        }
        String close = str(j, "close");
        if (close == null || close.isEmpty()) return null;

        return StockQuote.builder()
                .ticker(ticker)
                .name(str(j, "name"))
                .exchange(str(j, "exchange"))
                .currency(str(j, "currency"))
                .open(dbl(str(j, "open")))
                .high(dbl(str(j, "high")))
                .low(dbl(str(j, "low")))
                .close(dbl(close))
                .previousClose(dbl(str(j, "previous_close")))
                .volume(lng(str(j, "volume")))
                .change(dbl(str(j, "change")))
                .percentChange(dbl(str(j, "percent_change")))
                .marketOpen(j.has("is_market_open") && j.get("is_market_open").getAsBoolean())
                .build();
    }

    // ── SYMBOL SEARCH ────────────────────────────────────────

    public List<SearchResult> searchSymbols(String query, String apiKey) {
        if (query == null || query.length() < 1) return Collections.emptyList();

        String cacheKey = SEARCH_CACHE_PREFIX + query.toLowerCase().trim();
        String cached = safeGet(cacheKey);
        if (cached != null) {
            try { return Arrays.asList(gson.fromJson(cached, SearchResult[].class)); }
            catch (Exception e) { /* fall through */ }
        }

        if (apiKey == null || apiKey.isBlank()) return Collections.emptyList();

        try {
            String body = webClient.get()
                    .uri(u -> u.path("/symbol_search")
                            .queryParam("symbol", query.trim())
                            .queryParam("exchange", "EGX")
                            .queryParam("outputsize", "15")
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));

            if (body == null) return Collections.emptyList();
            JsonObject json = gson.fromJson(body, JsonObject.class);
            if (!json.has("data") || !json.get("data").isJsonArray()) return Collections.emptyList();

            List<SearchResult> results = new ArrayList<>();
            for (JsonElement el : json.getAsJsonArray("data")) {
                JsonObject item = el.getAsJsonObject();
                if (!"EGX".equalsIgnoreCase(str(item, "exchange"))) continue;
                results.add(SearchResult.builder()
                        .ticker(str(item, "symbol"))
                        .name(str(item, "instrument_name"))
                        .exchange("EGX")
                        .type(str(item, "instrument_type"))
                        .build());
            }

            if (!results.isEmpty()) safeSet(cacheKey, gson.toJson(results), Duration.ofHours(1));
            return results;
        } catch (Exception e) {
            log.error("Twelve Data search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── API KEY VALIDATION ───────────────────────────────────

    public StockQuote validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) return null;
        Map<String, StockQuote> r = fetchBatch(List.of("COMI"), apiKey);
        return r.get("COMI");
    }

    // ── REDIS HELPERS ────────────────────────────────────────

    private String safeGet(String key) {
        try { return redisTemplate.opsForValue().get(key); }
        catch (Exception e) { return null; }
    }
    private void safeSet(String key, String val, Duration ttl) {
        try { redisTemplate.opsForValue().set(key, val, ttl); }
        catch (Exception e) { log.debug("Redis set failed: {}", e.getMessage()); }
    }

    // ── PARSE HELPERS ────────────────────────────────────────

    private String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : null;
    }
    private double dbl(String v) {
        if (v == null || v.isEmpty()) return 0;
        try { return Double.parseDouble(v); } catch (Exception e) { return 0; }
    }
    private long lng(String v) {
        if (v == null || v.isEmpty()) return 0;
        try { return Long.parseLong(v); } catch (Exception e) { return 0; }
    }

    // ── DTOs ─────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StockQuote implements Serializable {
        private String ticker;
        private String name;
        private String exchange;
        private String currency;
        private double open;
        private double high;
        private double low;
        private double close;
        private double previousClose;
        private long volume;
        private double change;
        private double percentChange;
        private boolean marketOpen;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SearchResult implements Serializable {
        private String ticker;
        private String name;
        private String exchange;
        private String type;
    }
}
