package com.chatapp.chat_backend.dto.response;

import com.chatapp.chat_backend.model.message.MessageType;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID chatRoomId,
        UUID senderId,
        String senderUsername,
        MessageType messageType,
        String content,
        long readCount,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {}

