package com.chatapp.chat_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record CreateGroupChatRequest(
        @NotBlank(message = "Group name is required")
        String name,
        String description,
        List<UUID> memberIds
) {}

