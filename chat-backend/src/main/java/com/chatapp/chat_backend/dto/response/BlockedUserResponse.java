package com.chatapp.chat_backend.dto.response;

import java.time.Instant;
import java.util.UUID;

public record BlockedUserResponse(
        UUID id,
        UUID blockedUserId,
        String blockedUsername,
        Instant createdAt
) {}

