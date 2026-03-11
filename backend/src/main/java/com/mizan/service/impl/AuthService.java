package com.mizan.service.impl;

import com.mizan.dto.request.GoogleSignInRequest;
import com.mizan.dto.request.RefreshTokenRequest;
import com.mizan.dto.response.AuthResponse;
import com.mizan.entity.Portfolio;
import com.mizan.entity.Session;
import com.mizan.entity.User;
import com.mizan.enums.AuditAction;
import com.mizan.enums.AuthProvider;
import com.mizan.repository.PortfolioRepository;
import com.mizan.repository.SessionRepository;
import com.mizan.repository.UserRepository;
import com.mizan.security.GoogleTokenVerifier;
import com.mizan.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final SessionRepository sessionRepo;
    private final PortfolioRepository portfolioRepo;
    private final JwtProvider jwtProvider;
    private final GoogleTokenVerifier googleVerifier;
    private final AuditService auditService;

    @Value("${mizan.jwt.access-token-expiry}")
    private long accessTokenExpiry;

    @Value("${mizan.jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    @Transactional
    public AuthResponse signInWithGoogle(GoogleSignInRequest request, String ipAddress) {
        // 1. Verify Google ID token server-side
        GoogleTokenVerifier.GoogleUserInfo googleUser = googleVerifier.verify(request.getIdToken());

        // 2. Find or create user
        boolean isNew = false;
        User user = userRepo.findByAuthProviderAndProviderUserId(AuthProvider.GOOGLE, googleUser.getSub())
                .orElse(null);

        if (user == null) {
            isNew = true;
            user = User.builder()
                    .email(googleUser.getEmail())
                    .displayName(googleUser.getName())
                    .authProvider(AuthProvider.GOOGLE)
                    .providerUserId(googleUser.getSub())
                    .profileImageUrl(googleUser.getPicture())
                    .biometricEnabled(false)
                    .isActive(true)
                    .build();
            user = userRepo.save(user);

            // Create default portfolio
            Portfolio portfolio = Portfolio.builder()
                    .user(user)
                    .name("My Portfolio")
                    .isDefault(true)
                    .build();
            portfolioRepo.save(portfolio);

            log.info("New user registered via Google: {} ({})", user.getEmail(), user.getId());
        }

        // 3. Update last login
        user.setLastLoginAt(Instant.now());
        userRepo.save(user);

        // 4. Generate tokens and create session
        AuthResponse response = createAuthSession(user, request.getDeviceId(), request.getDeviceInfo(), ipAddress);
        response.setNewUser(isNew);

        // 5. Audit log
        auditService.logAsync(user.getId(),
                isNew ? AuditAction.SIGN_UP : AuditAction.SIGN_IN,
                ipAddress, request.getDeviceInfo(),
                Map.of("provider", "GOOGLE", "email", user.getEmail()));

        return response;
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, String ipAddress) {
        String hash = hashToken(request.getRefreshToken());
        Session session = sessionRepo.findByRefreshTokenHashAndRevokedAtIsNull(hash)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found or revoked. Possible token theft. IP: {}", ipAddress);
                    return new SecurityException("Invalid or expired refresh token");
                });

        if (session.isExpired()) {
            log.warn("Expired refresh token used by user {}. IP: {}", session.getUserId(), ipAddress);
            throw new SecurityException("Refresh token expired");
        }

        // Rotate: revoke old, create new
        session.setRevokedAt(Instant.now());
        sessionRepo.save(session);

        User user = userRepo.findById(session.getUserId())
                .orElseThrow(() -> new SecurityException("User not found"));

        user.setLastLoginAt(Instant.now());
        userRepo.save(user);

        auditService.logAsync(user.getId(), AuditAction.TOKEN_REFRESH, ipAddress, null, null);

        return createAuthSession(user, session.getDeviceId(), session.getDeviceInfo(), ipAddress);
    }

    @Transactional
    public void signOut(UUID userId, String refreshToken, String ipAddress) {
        if (refreshToken != null) {
            String hash = hashToken(refreshToken);
            sessionRepo.findByRefreshTokenHashAndRevokedAtIsNull(hash)
                    .ifPresent(session -> {
                        session.setRevokedAt(Instant.now());
                        sessionRepo.save(session);
                    });
        }
        auditService.logAsync(userId, AuditAction.SIGN_OUT, ipAddress, null, null);
        log.info("User {} signed out", userId);
    }

    private AuthResponse createAuthSession(User user, String deviceId, String deviceInfo, String ipAddress) {
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken();

        Session session = Session.builder()
                .userId(user.getId())
                .refreshTokenHash(hashToken(refreshToken))
                .deviceId(deviceId)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .expiresAt(Instant.now().plusSeconds(refreshTokenExpiry))
                .createdAt(Instant.now())
                .build();
        sessionRepo.save(session);

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .authProvider(user.getAuthProvider().name())
                .profileImageUrl(user.getProfileImageUrl())
                .biometricEnabled(user.isBiometricEnabled())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(accessTokenExpiry)
                .build();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
