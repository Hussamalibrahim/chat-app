package com.chatapp.chat_backend.dto.request;

import com.chatapp.chat_backend.model.message.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendMessageRequest(
        @NotNull(message = "Message type is required")
        MessageType messageType,
        @NotBlank(message = "Message content is required")
        String content
) {}

