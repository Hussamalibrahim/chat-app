package com.chatapp.chat_backend.dto.response;

import com.chatapp.chat_backend.model.chat.GroupRole;

import java.time.Instant;
import java.util.UUID;

public record ChatMemberResponse(
        UUID userId,
        String username,
        GroupRole role,
        Instant joinedAt
) {}

