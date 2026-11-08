package com.chatapp.chat_backend.dto.response;

import java.time.Instant;
import java.util.UUID;

public record MessageReadResponse(
        UUID id,
        UUID messageId,
        UUID userId,
        Instant readAt
) {}

