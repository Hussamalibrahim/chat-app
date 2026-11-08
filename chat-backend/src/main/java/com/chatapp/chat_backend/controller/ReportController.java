package com.chatapp.chat_backend.controller;

import com.chatapp.chat_backend.dto.request.CreateReportRequest;
import com.chatapp.chat_backend.dto.response.ReportResponse;
import com.chatapp.chat_backend.security.authentication.UserPrincipal;
import com.chatapp.chat_backend.service.report.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ReportResponse> createReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateReportRequest request) {
        return reportService.createReport(principal.getId(), request);
    }
}
