package com.mizan.security;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
public class GoogleTokenVerifier {

    @Value("${mizan.auth.google.client-id}")
    private String googleClientId;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://oauth2.googleapis.com")
            .build();

    private final Gson gson = new Gson();

    /**
     * Verifies Google ID token and returns user info.
     * In production, use Google's official java client library for more robust verification.
     */
    public GoogleUserInfo verify(String idToken) {
        try {
            String response = webClient.get()
                    .uri("/tokeninfo?id_token={token}", idToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonObject json = gson.fromJson(response, JsonObject.class);

            // Validate audience matches our client ID
            String aud = json.get("aud").getAsString();
            if (!googleClientId.equals(aud)) {
                log.warn("Google token audience mismatch. Expected: {}, Got: {}", googleClientId, aud);
                throw new SecurityException("Invalid Google token audience");
            }

            // Validate issuer
            String iss = json.get("iss").getAsString();
            if (!"accounts.google.com".equals(iss) && !"https://accounts.google.com".equals(iss)) {
                throw new SecurityException("Invalid Google token issuer");
            }

            // Check email verified
            boolean emailVerified = "true".equals(json.get("email_verified").getAsString());
            if (!emailVerified) {
                throw new SecurityException("Google email not verified");
            }

            return GoogleUserInfo.builder()
                    .sub(json.get("sub").getAsString())
                    .email(json.get("email").getAsString())
                    .name(json.has("name") ? json.get("name").getAsString() : json.get("email").getAsString())
                    .picture(json.has("picture") ? json.get("picture").getAsString() : null)
                    .build();

        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to verify Google token: {}", e.getMessage());
            throw new SecurityException("Google token verification failed", e);
        }
    }

    @lombok.Builder
    @lombok.Getter
    public static class GoogleUserInfo {
        private String sub;
        private String email;
        private String name;
        private String picture;
    }
}
