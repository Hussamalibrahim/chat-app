package com.chatapp.chat_backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateGroupChatRequest(
        @NotBlank(message = "Group name is required")
        String name,
        String description
) {}

