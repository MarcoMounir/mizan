package com.mizan.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    @Value("${mizan.rate-limit.auth-requests-per-minute}")
    private int authLimit;

    @Value("${mizan.rate-limit.api-requests-per-minute}")
    private int apiLimit;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Skip rate limiting for actuator
        if (path.contains("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);

        boolean isAuthEndpoint = path.startsWith("/api/auth/");
        int limit = isAuthEndpoint ? authLimit : apiLimit;
        String key = "rate:" + (isAuthEndpoint ? "auth:" : "api:") + clientIp;

        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofMinutes(1));
            }

            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - (count != null ? count : 0))));

            if (count != null && count > limit) {
                log.warn("Rate limit exceeded for IP {} on path {} (count: {})", clientIp, path, count);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Rate limit exceeded. Try again in 1 minute.\"}");
                return;
            }
        } catch (Exception e) {
            // Redis down — allow request (fail open), but log
            log.error("Redis rate-limit check failed, allowing request: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
