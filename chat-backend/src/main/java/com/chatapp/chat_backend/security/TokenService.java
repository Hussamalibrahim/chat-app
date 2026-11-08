package com.chatapp.chat_backend.security;

import com.chatapp.chat_backend.exception.BusinessException;
import com.chatapp.chat_backend.exception.ErrorCode;
import com.chatapp.chat_backend.model.RefreshToken;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class TokenService {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.ERROR_CREATING_TOKEN);
        }
    }

    public boolean isExpired(RefreshToken token) {
        return token.getExpiresAt() == null || !token.getExpiresAt().isAfter(Instant.now());
    }

    public boolean isRevoked(RefreshToken token) {
        return token.getRevokedAt() != null;
    }

    public boolean isValid(RefreshToken token) {
        return !isRevoked(token) && !isExpired(token);
    }
}
