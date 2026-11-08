package com.chatapp.chat_backend.dto.response;

public record AdminDashboardStatsResponse(
        long totalUsers,
        long activeUsers,
        long totalMessages,
        long messagesToday,
        long totalChatRooms,
        long totalGroupChats,
        long totalDirectChats,
        long totalReports,
        long pendingReports
) {}

