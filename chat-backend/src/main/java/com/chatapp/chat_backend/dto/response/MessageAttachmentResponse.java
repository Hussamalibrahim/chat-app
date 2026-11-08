package com.chatapp.chat_backend.dto.response;

import java.time.Instant;
import java.util.UUID;

public record MessageAttachmentResponse(
        UUID id,
        UUID messageId,
        String fileName,
        String fileType,
        Long fileSize,
        String fileUrl,
        Instant createdAt
) {}

