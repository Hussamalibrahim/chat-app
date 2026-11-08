package com.chatapp.chat_backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateMessageRequest(
        @NotBlank(message = "Message content is required")
        String content
) {}

