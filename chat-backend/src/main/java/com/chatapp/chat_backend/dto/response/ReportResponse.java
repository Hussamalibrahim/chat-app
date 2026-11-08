package com.chatapp.chat_backend.dto.response;

import com.chatapp.chat_backend.model.report.ReportDecision;

import java.time.Instant;
import java.util.UUID;

public record ReportResponse(
                UUID id,
                UUID reporterId,
                String reporterUsername,
                UUID reportedUserId,
                String reportedUsername,
                UUID messageId,
                String reason,
                String details,
                boolean reviewed,
                ReportDecision decision,
                String adminNotes,
                Instant reviewedAt,
                Instant createdAt) {
}
