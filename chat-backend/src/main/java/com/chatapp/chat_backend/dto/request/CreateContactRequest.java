package com.chatapp.chat_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateContactRequest(
        @NotNull(message = "Contact user ID is required")
        UUID contactUserId,
        String alias
) {}

