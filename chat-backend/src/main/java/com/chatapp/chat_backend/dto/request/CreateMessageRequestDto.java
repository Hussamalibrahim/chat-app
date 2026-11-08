package com.chatapp.chat_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateMessageRequestDto(
        @NotNull(message = "Target user ID is required")
        UUID targetUserId
) {}

