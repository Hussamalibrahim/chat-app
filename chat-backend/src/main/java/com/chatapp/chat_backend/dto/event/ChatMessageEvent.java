package com.chatapp.chat_backend.dto.event;

import com.chatapp.chat_backend.model.message.MessageType;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageEvent(
        UUID eventId,
        String eventType,
        UUID messageId,
        UUID chatRoomId,
        UUID senderId,
        String content,
        MessageType messageType,
        Instant timestamp
) {}

