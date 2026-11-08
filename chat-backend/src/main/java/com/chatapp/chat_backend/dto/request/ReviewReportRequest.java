package com.chatapp.chat_backend.dto.request;

import com.chatapp.chat_backend.model.report.ReportDecision;
import jakarta.validation.constraints.NotNull;

public record ReviewReportRequest(
        @NotNull(message = "Decision is required")
        ReportDecision decision,
        String adminNotes
) {}

