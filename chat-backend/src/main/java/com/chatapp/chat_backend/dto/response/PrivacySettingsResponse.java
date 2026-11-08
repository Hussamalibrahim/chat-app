package com.chatapp.chat_backend.dto.response;

import com.chatapp.chat_backend.model.privacy.WhoCanAddToGroups;
import com.chatapp.chat_backend.model.privacy.WhoCanMessage;

import java.time.Instant;
import java.util.UUID;

public record PrivacySettingsResponse(
        UUID userId,
        WhoCanMessage whoCanMessage,
        WhoCanAddToGroups whoCanAddToGroups,
        Instant updatedAt
) {}

