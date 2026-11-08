package com.chatapp.chat_backend.dto.response;

import java.time.Instant;
import java.util.UUID;

public record DirectChatResponse(
        UUID chatRoomId,
        UUID otherUserId,
        String otherUsername,
        Instant createdAt
) {}

