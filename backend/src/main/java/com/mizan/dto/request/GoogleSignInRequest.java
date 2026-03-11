package com.mizan.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleSignInRequest {
    @NotBlank(message = "Google ID token is required")
    private String idToken;
    private String deviceId;
    private String deviceInfo;
}
