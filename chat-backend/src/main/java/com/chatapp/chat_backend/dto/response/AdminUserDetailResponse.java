package com.chatapp.chat_backend.dto.response;

import com.chatapp.chat_backend.model.Role;
import com.chatapp.chat_backend.model.User;

import java.time.Instant;
import java.util.UUID;

public record AdminUserDetailResponse(
        UUID id,
        String username,
        String email,
        String phone,
        Role role,
        Instant emailVerifiedAt,
        Instant phoneVerifiedAt,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        boolean isSuspended
) {
    public static AdminUserDetailResponse from(User user) {
        return new AdminUserDetailResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getEmailVerifiedAt(),
                user.getPhoneVerifiedAt(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getDeletedAt(),
                user.getDeletedAt() != null
        );
    }
}

