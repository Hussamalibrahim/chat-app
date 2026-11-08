package com.chatapp.chat_backend.dto.response;

import com.chatapp.chat_backend.model.group.InvitationStatus;

import java.time.Instant;
import java.util.UUID;

public record GroupInvitationResponse(
        UUID id,
        UUID groupChatId,
        String groupName,
        UUID inviterId,
        String inviterUsername,
        UUID inviteeId,
        InvitationStatus status,
        Instant createdAt,
        Instant updatedAt
) {}

