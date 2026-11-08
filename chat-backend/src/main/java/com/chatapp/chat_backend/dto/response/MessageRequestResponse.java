package com.chatapp.chat_backend.dto.response;

import com.chatapp.chat_backend.model.message.RequestStatus;

import java.time.Instant;
import java.util.UUID;

public record MessageRequestResponse(
        UUID id,
        UUID senderId,
        String senderUsername,
        UUID receiverId,
        String receiverUsername,
        RequestStatus status,
        Instant createdAt,
        Instant updatedAt
) {}

