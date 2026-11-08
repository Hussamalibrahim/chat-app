package com.chatapp.chat_backend.dto.request;

import com.chatapp.chat_backend.model.chat.GroupRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull(message = "Role is required")
        GroupRole role
) {}

