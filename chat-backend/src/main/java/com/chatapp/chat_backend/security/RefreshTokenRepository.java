package com.chatapp.chat_backend.security;

import com.chatapp.chat_backend.model.RefreshToken;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RefreshTokenRepository extends ReactiveCrudRepository<RefreshToken, UUID> {

    Mono<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
        UPDATE refresh_tokens
        SET revoked_at = CURRENT_TIMESTAMP
        WHERE id = :id
          AND revoked_at IS NULL
          AND expires_at > CURRENT_TIMESTAMP
    """)
    Mono<Integer> revokeIfActive(UUID id);

    @Modifying
    @Query("""
        UPDATE refresh_tokens
        SET revoked_at = CURRENT_TIMESTAMP
        WHERE token_hash = :tokenHash
          AND revoked_at IS NULL
    """)
    Mono<Integer> revokeByTokenHash(String tokenHash);

    @Modifying
    @Query("""
        UPDATE refresh_tokens
        SET revoked_at = CURRENT_TIMESTAMP
        WHERE user_id = :userId
          AND revoked_at IS NULL
    """)
    Mono<Integer> revokeAllByUserId(UUID userId);

    @Modifying
    @Query("""
        DELETE FROM refresh_tokens
        WHERE expires_at < CURRENT_TIMESTAMP
    """)
    Mono<Integer> deleteExpiredTokens();
}
