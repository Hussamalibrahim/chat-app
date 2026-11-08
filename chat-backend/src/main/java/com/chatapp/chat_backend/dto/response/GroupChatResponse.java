package com.chatapp.chat_backend.dto.response;

import java.time.Instant;
import java.util.UUID;

public record GroupChatResponse(
        UUID chatRoomId,
        String name,
        String description,
        UUID createdBy,
        long memberCount,
        Instant createdAt,
        Instant updatedAt
) {}

