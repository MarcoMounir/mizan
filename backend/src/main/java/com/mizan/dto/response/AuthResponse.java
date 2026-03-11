package com.mizan.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data @Builder
public class AuthResponse {
    private UUID userId;
    private String email;
    private String displayName;
    private String authProvider;
    private String profileImageUrl;
    private boolean biometricEnabled;
    private String accessToken;
    private String refreshToken;
    private long accessTokenExpiresIn;  // seconds
    private boolean isNewUser;
}
