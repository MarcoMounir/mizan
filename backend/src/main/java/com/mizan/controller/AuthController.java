package com.mizan.controller;

import com.mizan.dto.request.GoogleSignInRequest;
import com.mizan.dto.request.RefreshTokenRequest;
import com.mizan.dto.response.AuthResponse;
import com.mizan.service.impl.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> signInWithGoogle(
            @Valid @RequestBody GoogleSignInRequest request,
            HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        log.info("Google sign-in attempt from IP: {}", ip);
        AuthResponse response = authService.signInWithGoogle(request, ip);
        return ResponseEntity.ok(response);
    }

    // Apple sign-in follows the same pattern — would add AppleSignInRequest + AppleTokenVerifier
    // @PostMapping("/apple")
    // public ResponseEntity<AuthResponse> signInWithApple(...)

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.refreshToken(request, getClientIp(httpRequest));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signout")
    public ResponseEntity<Map<String, String>> signOut(
            @AuthenticationPrincipal UUID userId,
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        authService.signOut(userId,
                request != null ? request.getRefreshToken() : null,
                getClientIp(httpRequest));
        return ResponseEntity.ok(Map.of("message", "Signed out successfully"));
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isEmpty()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }
}
