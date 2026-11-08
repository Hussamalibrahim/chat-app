package com.chatapp.chat_backend.controller;

import com.chatapp.chat_backend.dto.request.ReviewReportRequest;
import com.chatapp.chat_backend.dto.response.AdminDashboardStatsResponse;
import com.chatapp.chat_backend.dto.response.AdminUserDetailResponse;
import com.chatapp.chat_backend.dto.response.ReportResponse;
import com.chatapp.chat_backend.security.authentication.UserPrincipal;
import com.chatapp.chat_backend.service.admin.AdminService;
import com.chatapp.chat_backend.service.report.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ReportService reportService;

    @GetMapping("/dashboard")
    public Mono<AdminDashboardStatsResponse> getDashboard() {
        return adminService.getDashboardStats();
    }

    @GetMapping("/statistics")
    public Mono<AdminDashboardStatsResponse> getStatistics() {
        return adminService.getDashboardStats();
    }

    @GetMapping("/users")
    public Flux<AdminUserDetailResponse> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.listUsers(page, size);
    }

    @GetMapping("/users/search")
    public Flux<AdminUserDetailResponse> searchUsers(@RequestParam String query) {
        return adminService.searchUsers(query);
    }

    @GetMapping("/users/{userId}")
    public Mono<AdminUserDetailResponse> getUserDetails(@PathVariable UUID userId) {
        return adminService.getUserDetails(userId);
    }

    @PatchMapping("/users/{userId}/suspend")
    public Mono<AdminUserDetailResponse> suspendUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "true") boolean suspend) {
        return adminService.suspendUser(userId, suspend);
    }

    @GetMapping("/reports")
    public Flux<ReportResponse> listReports(
            @RequestParam(required = false) String reason,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reportService.getReports(reason, page, size);
    }

    @GetMapping("/reports/{reportId}")
    public Mono<ReportResponse> getReportDetails(@PathVariable UUID reportId) {
        return reportService.getReportById(reportId);
    }

    @PostMapping("/reports/{reportId}/review")
    public Mono<ReportResponse> reviewReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID reportId,
            @Valid @RequestBody ReviewReportRequest request) {
        return reportService.reviewReport(principal.getId(), reportId, request);
    }
}
