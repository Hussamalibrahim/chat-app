package com.chatapp.chat_backend.service;

import com.chatapp.chat_backend.exception.BusinessException;
import com.chatapp.chat_backend.exception.ErrorCode;
import com.chatapp.chat_backend.model.Role;
import com.chatapp.chat_backend.model.User;
import com.chatapp.chat_backend.repository.UserRepository;
import com.chatapp.chat_backend.repository.chat.ChatRoomRepository;
import com.chatapp.chat_backend.repository.message.MessageRepository;
import com.chatapp.chat_backend.repository.report.UserReportRepository;
import com.chatapp.chat_backend.service.admin.AdminService;
import com.chatapp.chat_backend.util.TestStubs;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AdminServiceTest {

    @Test
    void getDashboardStats_CalculatesCorrectly() {
        UserRepository userRepo = TestStubs.of(UserRepository.class)
                .on("countTotalActiveUsers", args -> Mono.just(100L))
                .on("countActiveMessengersSince", args -> Mono.just(45L))
                .build();

        MessageRepository messageRepo = TestStubs.of(MessageRepository.class)
                .on("countTotalMessages", args -> Mono.just(5000L))
                .on("countMessagesSince", args -> Mono.just(250L))
                .build();

        ChatRoomRepository chatRoomRepo = TestStubs.of(ChatRoomRepository.class)
                .on("countTotalRooms", args -> Mono.just(80L))
                .on("countGroupRooms", args -> Mono.just(30L))
                .on("countDirectRooms", args -> Mono.just(50L))
                .build();

        UserReportRepository reportRepo = TestStubs.of(UserReportRepository.class)
                .on("countTotalReports", args -> Mono.just(12L))
                .on("countPendingReports", args -> Mono.just(3L))
                .build();

        AdminService adminService = new AdminService(userRepo, messageRepo, chatRoomRepo, reportRepo);

        StepVerifier.create(adminService.getDashboardStats())
                .assertNext(res -> {
                    assertEquals(100L, res.totalUsers());
                    assertEquals(45L, res.activeUsers());
                    assertEquals(5000L, res.totalMessages());
                    assertEquals(250L, res.messagesToday());
                    assertEquals(80L, res.totalChatRooms());
                    assertEquals(30L, res.totalGroupChats());
                    assertEquals(50L, res.totalDirectChats());
                    assertEquals(12L, res.totalReports());
                    assertEquals(3L, res.pendingReports());
                })
                .verifyComplete();
    }

    @Test
    void suspendUser_Success() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("baduser").role(Role.USER).build();

        UserRepository userRepo = TestStubs.of(UserRepository.class)
                .on("findById", args -> Mono.just(user))
                .on("save", args -> Mono.just(args[0]))
                .build();

        AdminService adminService = new AdminService(
                userRepo,
                TestStubs.of(MessageRepository.class).build(),
                TestStubs.of(ChatRoomRepository.class).build(),
                TestStubs.of(UserReportRepository.class).build()
        );

        StepVerifier.create(adminService.suspendUser(userId, true))
                .assertNext(res -> {
                    assertTrue(res.isSuspended());
                    assertNotNull(res.deletedAt());
                })
                .verifyComplete();
    }

    @Test
    void searchUsers_BlankQuery_ReturnsEmptyFlux() {
        UserRepository userRepo = TestStubs.of(UserRepository.class)
                .build();

        AdminService adminService = new AdminService(
                userRepo,
                TestStubs.of(MessageRepository.class).build(),
                TestStubs.of(ChatRoomRepository.class).build(),
                TestStubs.of(UserReportRepository.class).build()
        );

        StepVerifier.create(adminService.searchUsers("   "))
                .verifyComplete();
    }

    @Test
    void getUserDetails_DeletedUser_IsNotFound() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("ghost")
                .role(Role.USER)
                .deletedAt(Instant.now())
                .build();

        UserRepository userRepo = TestStubs.of(UserRepository.class)
                .on("findById", args -> Mono.just(user))
                .build();

        AdminService adminService = new AdminService(
                userRepo,
                TestStubs.of(MessageRepository.class).build(),
                TestStubs.of(ChatRoomRepository.class).build(),
                TestStubs.of(UserReportRepository.class).build()
        );

        StepVerifier.create(adminService.getUserDetails(userId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException
                        && ((BusinessException) throwable).getErrorCode() == ErrorCode.USER_NOT_FOUND)
                .verify();
    }
}
