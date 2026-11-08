package com.chatapp.chat_backend.service;

import com.chatapp.chat_backend.dto.request.ChangePasswordRequest;
import com.chatapp.chat_backend.dto.request.RegisterUserRequest;
import com.chatapp.chat_backend.dto.response.UserResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserService {

    Mono<UserResponse> register(RegisterUserRequest request);

    Mono<UserResponse> addContact(UUID userId, String email, String phone);

    Mono<UserResponse> findUser(UUID id, String username, String email, String phone);

    Mono<UserResponse> updateUser(UUID userId, String username);

    Mono<Void> changePassword(UUID userId, ChangePasswordRequest request);

    Mono<UserResponse> getUserById(UUID id);

    Flux<UserResponse> searchUsers(String query);

    Mono<Void> softDeleteUser(UUID userId);
}

