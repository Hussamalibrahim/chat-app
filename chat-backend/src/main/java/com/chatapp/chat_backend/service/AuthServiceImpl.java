package com.chatapp.chat_backend.service;

import com.chatapp.chat_backend.dto.request.LoginRequest;
import com.chatapp.chat_backend.dto.request.RefreshTokenRequest;
import com.chatapp.chat_backend.dto.response.AuthResponse;
import com.chatapp.chat_backend.exception.BusinessException;
import com.chatapp.chat_backend.exception.ErrorCode;
import com.chatapp.chat_backend.model.RefreshToken;
import com.chatapp.chat_backend.security.RefreshTokenRepository;
import com.chatapp.chat_backend.repository.UserRepository;
import com.chatapp.chat_backend.security.TokenService;
import com.chatapp.chat_backend.security.authentication.UserPrincipal;
import com.chatapp.chat_backend.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private final ReactiveAuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Override
    public Mono<AuthResponse> login(LoginRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken =
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.identifier(),
                        request.password()
                );

        return authenticationManager.authenticate(authenticationToken)
                .flatMap(this::generateAuthResponse)
                .onErrorMap(
                        AuthenticationException.class, ex -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
    }

    @Override
    public Mono<AuthResponse> refresh(RefreshTokenRequest request) {
        if (request.refreshToken() == null || request.refreshToken().isBlank()) {
            return Mono.error(new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        }

        String tokenHash = tokenService.hashToken(request.refreshToken());

        return refreshTokenRepository.findByTokenHash(tokenHash)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)))
                .flatMap(token -> {
                    // 1. Reuse detection: if an already revoked token is used, suspect breach and revoke all user tokens
                    if (tokenService.isRevoked(token)) {
                        log.warn("Token reuse detected for userId: {}. Revoking all tokens.", token.getUserId());
                        return refreshTokenRepository.revokeAllByUserId(token.getUserId())
                                .then(Mono.error(new BusinessException(ErrorCode.TOKEN_REUSE_DETECTED)));
                    }
                    // 2. Expiration check
                    if (tokenService.isExpired(token)) {
                        return Mono.error(new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN));
                    }
                    // 3. Token is valid -> perform rotation
                    return rotateToken(token);
                });
    }

    @Override
    public Mono<Void> logout(RefreshTokenRequest request) {
        if (request.refreshToken() == null || request.refreshToken().isBlank()) {
            return Mono.empty();
        }

        String tokenHash = tokenService.hashToken(request.refreshToken());
        return refreshTokenRepository.revokeByTokenHash(tokenHash)
                .then();
    }

    @Override
    public Mono<Void> logoutAll(UUID userId) {
        if (userId == null) {
            return Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND));
        }

        return refreshTokenRepository.revokeAllByUserId(userId)
                .then();
    }

    private Mono<AuthResponse> generateAuthResponse(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String accessToken = jwtService.generateToken(principal);

        String rawRefreshToken = tokenService.generateToken();
        String refreshTokenHash = tokenService.hashToken(rawRefreshToken);

        Instant now = Instant.now();
        RefreshToken token = RefreshToken.builder()
                .userId(principal.getId())
                .tokenHash(refreshTokenHash)
                .expiresAt(now.plusMillis(refreshExpiration))
                .createdAt(now)
                .build();

        return refreshTokenRepository.save(token)
                .map(saved -> new AuthResponse(
                        accessToken,
                        rawRefreshToken,
                        "Bearer",
                        jwtService.getExpiration()
                ));
    }

    private Mono<AuthResponse> rotateToken(RefreshToken oldToken) {
        return userRepository.findByIdAndDeletedAtIsNull(oldToken.getUserId())
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)))
                .flatMap(user ->
                        refreshTokenRepository.revokeIfActive(oldToken.getId())
                                .flatMap(rowsUpdated -> {
                                    if (rowsUpdated <= 0) {
                                        return Mono.error(new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
                                    }

                                    String newRefreshToken = tokenService.generateToken();
                                    Instant now = Instant.now();
                                    RefreshToken newToken = RefreshToken.builder()
                                            .userId(user.getId())
                                            .tokenHash(tokenService.hashToken(newRefreshToken))
                                            .expiresAt(now.plusMillis(refreshExpiration))
                                            .createdAt(now)
                                            .build();

                                    String newAccessToken = jwtService.generateToken(new UserPrincipal(user));

                                    return refreshTokenRepository.save(newToken)
                                            .map(saved -> new AuthResponse(
                                                    newAccessToken,
                                                    newRefreshToken,
                                                    "Bearer",
                                                    jwtService.getExpiration()
                                            ));
                                })
                );
    }
}
