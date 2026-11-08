package com.chatapp.chat_backend.dto.event;

import java.time.Instant;
import java.util.UUID;

public record NotificationEvent(
        UUID eventId,
        UUID recipientId,
        String title,
        String body,
        UUID chatRoomId,
        Instant timestamp
) {}

