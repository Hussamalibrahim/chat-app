package com.chatapp.chat_backend.security;

import com.chatapp.chat_backend.model.RefreshToken;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private final TokenService tokenService = new TokenService();

    @Test
    void testTokenGeneration() {
        String token1 = tokenService.generateToken();
        String token2 = tokenService.generateToken();

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
        assertTrue(token1.length() >= 32);
    }

    @Test
    void testTokenHashing() {
        String token = "my-secret-refresh-token";
        String hash1 = tokenService.hashToken(token);
        String hash2 = tokenService.hashToken(token);

        assertNotNull(hash1);
        assertEquals(hash1, hash2);
    }

    @Test
    void testTokenValidity() {
        Instant now = Instant.now();
        RefreshToken validToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .tokenHash("hash")
                .expiresAt(now.plusSeconds(3600))
                .createdAt(now)
                .build();

        assertTrue(tokenService.isValid(validToken));
        assertFalse(tokenService.isExpired(validToken));
        assertFalse(tokenService.isRevoked(validToken));

        RefreshToken expiredToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .expiresAt(now.minusSeconds(10))
                .build();

        assertFalse(tokenService.isValid(expiredToken));
        assertTrue(tokenService.isExpired(expiredToken));

        RefreshToken revokedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .expiresAt(now.plusSeconds(3600))
                .revokedAt(now)
                .build();

        assertFalse(tokenService.isValid(revokedToken));
        assertTrue(tokenService.isRevoked(revokedToken));
    }
}
