package com.chatapp.chat_backend.service;

import com.chatapp.chat_backend.dto.request.LoginRequest;
import com.chatapp.chat_backend.dto.request.RefreshTokenRequest;
import com.chatapp.chat_backend.dto.response.AuthResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface AuthService {

    Mono<AuthResponse> login(LoginRequest request);

    Mono<AuthResponse> refresh(RefreshTokenRequest request);

    Mono<Void> logout(RefreshTokenRequest request);

    Mono<Void> logoutAll(UUID userId);
}
