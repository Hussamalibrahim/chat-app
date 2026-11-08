package com.chatapp.chat_backend.dto.request;

import com.chatapp.chat_backend.model.chat.GroupRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddGroupMemberRequest(
        @NotNull(message = "User ID is required")
        UUID userId,
        GroupRole role
) {}

