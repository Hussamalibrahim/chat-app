package com.chatapp.chat_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateReportRequest(
        @NotNull(message = "Reported user ID is required")
        UUID reportedUserId,
        UUID messageId,
        @NotBlank(message = "Reason is required")
        String reason,
        String details
) {}

