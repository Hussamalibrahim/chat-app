package com.chatapp.chat_backend.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ContactResponse(
                UUID id,
                UUID contactUserId,
                String contactUsername,
                String contactEmail,
                String contactPhone,
                String alias,
                Instant createdAt) {
}
